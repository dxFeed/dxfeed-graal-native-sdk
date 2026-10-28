// Copyright (c) 2026 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

package com.dxfeed.sdk.subscription;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import com.dxfeed.api.DXEndpoint;
import com.dxfeed.api.DXFeedSubscription;
import com.dxfeed.api.DXPublisher;
import com.dxfeed.event.EventType;
import com.dxfeed.event.market.Quote;
import com.dxfeed.event.market.TimeAndSale;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * {@link EventQueue} over a LOCAL_HUB endpoint. {@link EventQueue#closeAll()} cannot be undone through the API,
 * so every test undoes it by reflection afterwards.
 */
public class EventQueueTest {

    private static final long TIMEOUT_MILLIS = 10_000;
    // QD's agent buffer of a LOCAL_HUB endpoint holds 1,000,000 records and drops the oldest beyond that.
    private static final int BUFFER_SIZE = 1_000_000;

    private DXEndpoint endpoint;
    private DXPublisher publisher;

    @Before
    public void createEndpoint() {
        endpoint = DXEndpoint.create(DXEndpoint.Role.LOCAL_HUB);
        publisher = endpoint.getPublisher();
    }

    @After
    public void closeEndpointAndUndoCloseAll() throws ReflectiveOperationException, InterruptedException {
        // A queue left with pending tasks by a failed test would hang closeAndAwaitTermination.
        EventQueue.closeAll();
        undoCloseAll();
        endpoint.closeAndAwaitTermination();
    }

    private static void undoCloseAll() throws ReflectiveOperationException {
        final Field lock = EventQueue.class.getDeclaredField("REGISTRY_LOCK");
        final Field queues = EventQueue.class.getDeclaredField("QUEUES");
        final Field allClosed = EventQueue.class.getDeclaredField("allClosed");
        lock.setAccessible(true);
        queues.setAccessible(true);
        allClosed.setAccessible(true);
        synchronized (lock.get(null)) {
            ((Set<?>) queues.get(null)).clear();
            allClosed.setBoolean(null, false);
        }
    }

    @Test
    public void drainReturnsThePublishedEventsInOrder() throws InterruptedException {
        final DXFeedSubscription<Quote> subscription = endpoint.getFeed().createSubscription(Quote.class);
        final EventQueue queue = EventQueue.create(subscription);
        subscription.addSymbols("A", "B", "C");

        publisher.publishEvents(List.of(quote("A", 1), quote("B", 2), quote("C", 3)));

        final List<EventType<?>> events = queue.drain(TIMEOUT_MILLIS, 0);
        assertEquals(List.of("A", "B", "C"), symbols(events));
        assertEquals(0, queue.getDroppedEvents());
    }

    @Test
    public void lastingEventsAreConflatedUntilTheDrain() throws InterruptedException {
        final DXFeedSubscription<Quote> subscription = endpoint.getFeed().createSubscription(Quote.class);
        final EventQueue queue = EventQueue.create(subscription);
        subscription.addSymbols("A");

        for (int i = 1; i <= 10_000; i++) {
            publisher.publishEvents(List.of(quote("A", i)));
        }

        final List<EventType<?>> events = drainAll(queue, 0);
        assertTrue("events: " + events.size(), events.size() < 100);
        assertEquals(10_000, ((Quote) events.get(events.size() - 1)).getBidPrice(), 0);
    }

    @Test
    public void overflowGrowsTheDropTotalAndKeepsTheOrder() throws InterruptedException {
        final DXFeedSubscription<TimeAndSale> subscription = endpoint.getFeed().createSubscription(TimeAndSale.class);
        final EventQueue queue = EventQueue.create(subscription);
        subscription.addSymbols("A");

        final int total = BUFFER_SIZE + 50_000;
        publishTimeAndSales("A", total);

        final List<EventType<?>> events = drainAll(queue, 0);
        assertTrue("dropped: " + queue.getDroppedEvents(), queue.getDroppedEvents() >= total - BUFFER_SIZE);
        assertEquals(total, events.size() + queue.getDroppedEvents());
        assertAscendingPrices(events);
    }

    @Test
    public void drainReturnsNoEventsAfterTheTimeout() throws InterruptedException {
        final EventQueue queue = EventQueue.create(endpoint.getFeed().createSubscription(Quote.class));

        long start = System.nanoTime();
        assertEquals(List.of(), queue.drain(50, 0));
        final long waited = elapsedMillis(start);
        assertTrue("waited " + waited, waited >= 45 && waited < 5_000);

        start = System.nanoTime();
        assertEquals(List.of(), queue.drain(0, 0));
        assertTrue(elapsedMillis(start) < 1_000);
    }

    @Test
    public void wakeUpEndsTheNextOrTheCurrentDrain() throws Exception {
        final EventQueue queue = EventQueue.create(endpoint.getFeed().createSubscription(Quote.class));

        queue.wakeUp();
        long start = System.nanoTime();
        assertEquals(List.of(), queue.drain(TIMEOUT_MILLIS, 0));
        assertTrue(elapsedMillis(start) < 1_000);

        final AtomicReference<Object> result = new AtomicReference<>();
        final Thread drainer = startDrain(queue, TIMEOUT_MILLIS, result);
        awaitDrainInProgress(queue);
        start = System.nanoTime();
        queue.wakeUp();
        drainer.join(TIMEOUT_MILLIS);
        assertFalse(drainer.isAlive());
        assertTrue("woken after " + elapsedMillis(start), elapsedMillis(start) < 500);
        assertEquals(List.of(), result.get());
    }

    @Test
    public void subscriptionCloseEndsTheDrainWithClosed() throws Exception {
        final DXFeedSubscription<Quote> subscription = endpoint.getFeed().createSubscription(Quote.class);
        final EventQueue queue = EventQueue.create(subscription);
        assertClosedWakesTheDrain(queue, subscription::close);
    }

    @Test
    public void queueCloseEndsTheDrainWithClosed() throws Exception {
        final EventQueue queue = EventQueue.create(endpoint.getFeed().createSubscription(Quote.class));
        assertClosedWakesTheDrain(queue, queue::close);
    }

    @Test
    public void closeAllEndsTheDrainWithClosedAndClosesTheQueuesCreatedLater() throws Exception {
        final EventQueue queue = EventQueue.create(endpoint.getFeed().createSubscription(Quote.class));
        assertClosedWakesTheDrain(queue, EventQueue::closeAll);

        final EventQueue later = EventQueue.create(endpoint.getFeed().createSubscription(Quote.class));
        assertTrue(later.isClosed());
        assertNull(later.drain(TIMEOUT_MILLIS, 0));
    }

    @Test
    public void concurrentDrainsDoNotSplitABatch() throws Exception {
        final DXFeedSubscription<TimeAndSale> subscription = endpoint.getFeed().createSubscription(TimeAndSale.class);
        final EventQueue queue = EventQueue.create(subscription);
        subscription.addSymbols("A");

        final int total = 100_000;
        final ConcurrentLinkedQueue<List<EventType<?>>> batches = new ConcurrentLinkedQueue<>();
        final List<Thread> drainers = new ArrayList<>();
        for (int i = 0; i < 2; i++) {
            drainers.add(start(() -> {
                try {
                    List<EventType<?>> events;
                    while ((events = queue.drain(TIMEOUT_MILLIS, 1_000)) != null) {
                        if (!events.isEmpty()) {
                            batches.add(events);
                        }
                    }
                } catch (final InterruptedException e) {
                    throw new AssertionError(e);
                }
            }));
        }
        publishTimeAndSales("A", total);
        awaitCondition(() -> batches.stream().mapToInt(List::size).sum() >= total);
        queue.close();
        for (final Thread drainer : drainers) {
            drainer.join(TIMEOUT_MILLIS);
            assertFalse(drainer.isAlive());
        }

        final Set<Double> prices = new HashSet<>();
        for (final List<EventType<?>> batch : batches) {
            assertAscendingPrices(batch);
            for (final EventType<?> event : batch) {
                assertTrue("duplicate", prices.add(((TimeAndSale) event).getPrice()));
            }
        }
        assertEquals(total, prices.size());
    }

    @Test
    public void pushListenersRunOnTheDrainingThread() throws InterruptedException {
        final DXFeedSubscription<Quote> subscription = endpoint.getFeed().createSubscription(Quote.class);
        final EventQueue queue = EventQueue.create(subscription);
        final Set<Thread> listenerThreads = Collections.synchronizedSet(new HashSet<>());
        subscription.addEventListener(events -> listenerThreads.add(Thread.currentThread()));
        subscription.addSymbols("A");

        publisher.publishEvents(List.of(quote("A", 1)));

        assertEquals(1, queue.drain(TIMEOUT_MILLIS, 0).size());
        assertEquals(Set.of(Thread.currentThread()), listenerThreads);
    }

    @Test
    public void misuseFailsWithIllegalStateException() {
        final DXFeedSubscription<Quote> withSymbols = endpoint.getFeed().createSubscription(Quote.class);
        withSymbols.addSymbols("A");
        assertThrows(IllegalStateException.class, () -> EventQueue.create(withSymbols));

        final DXFeedSubscription<Quote> subscription = endpoint.getFeed().createSubscription(Quote.class);
        final EventQueue queue = EventQueue.create(subscription);
        assertSame(queue, EventQueue.of(subscription));
        assertThrows(IllegalStateException.class, () -> EventQueue.create(subscription));
        final Executor direct = Runnable::run;
        assertThrows(IllegalStateException.class,
                () -> EventQueue.setExecutor(subscription::getExecutor, subscription::setExecutor, direct));
        final var controller = subscription.getSubscriptionController();
        assertThrows(IllegalStateException.class,
                () -> EventQueue.setExecutor(controller::getExecutor, controller::setExecutor, direct));
        assertSame(queue, EventQueue.of(subscription));

        final DXFeedSubscription<Quote> other = endpoint.getFeed().createSubscription(Quote.class);
        assertThrows(IllegalStateException.class,
                () -> EventQueue.setExecutor(other::getExecutor, other::setExecutor, subscription.getExecutor()));
        EventQueue.setExecutor(other::getExecutor, other::setExecutor, direct);
        assertThrows(IllegalStateException.class, () -> EventQueue.create(other));
    }

    @Test
    public void drainFromAListenerOfItsOwnDrainFails() throws InterruptedException {
        final DXFeedSubscription<Quote> subscription = endpoint.getFeed().createSubscription(Quote.class);
        final EventQueue queue = EventQueue.create(subscription);
        final AtomicReference<Throwable> failure = new AtomicReference<>();
        subscription.addEventListener(events -> {
            try {
                queue.drain(0, 0);
            } catch (final Throwable e) {
                failure.set(e);
            }
        });
        subscription.addSymbols("A");

        publisher.publishEvents(List.of(quote("A", 1)));

        assertEquals(1, queue.drain(TIMEOUT_MILLIS, 0).size());
        assertTrue(String.valueOf(failure.get()), failure.get() instanceof IllegalStateException);
    }

    @Test
    public void droppedBatchIsCountedOnce() throws InterruptedException {
        final DXFeedSubscription<Quote> subscription = endpoint.getFeed().createSubscription(Quote.class);
        final EventQueue queue = EventQueue.create(subscription);
        subscription.addSymbols("A", "B");

        publisher.publishEvents(List.of(quote("A", 1), quote("B", 2)));
        queue.dropBatch(queue.drain(TIMEOUT_MILLIS, 0));
        assertEquals(2, queue.getDroppedEvents());

        publisher.publishEvents(List.of(quote("A", 3)));
        assertEquals(1, queue.drain(TIMEOUT_MILLIS, 0).size());
        assertEquals(2, queue.getDroppedEvents());
    }

    @Test
    public void closeWithUndrainedDataCountsItAndLetsTheEndpointClose() throws Exception {
        final DXFeedSubscription<Quote> subscription = endpoint.getFeed().createSubscription(Quote.class);
        final EventQueue queue = EventQueue.create(subscription);
        subscription.addSymbols("A", "B");
        publisher.publishEvents(List.of(quote("A", 1), quote("B", 2)));

        queue.close();
        assertEquals(2, queue.getDroppedEvents());

        // A closed queue runs a late task at once.
        publisher.publishEvents(List.of(quote("A", 3)));
        assertEquals(3, queue.getDroppedEvents());

        final Thread closer = start(() -> {
            try {
                endpoint.closeAndAwaitTermination();
            } catch (final InterruptedException e) {
                throw new AssertionError(e);
            }
        });
        closer.join(TIMEOUT_MILLIS);
        assertFalse("closeAndAwaitTermination hangs", closer.isAlive());
    }

    @Test
    public void detachAndClearReturnsWithoutADrain() throws Exception {
        final DXFeedSubscription<Quote> subscription = endpoint.getFeed().createSubscription(Quote.class);
        final EventQueue queue = EventQueue.create(subscription);
        subscription.addSymbols("A", "B");
        publisher.publishEvents(List.of(quote("A", 1), quote("B", 2)));

        final Thread detacher = start(() -> EventQueue.detachSubscriptionAndClear(endpoint.getFeed(), subscription));
        detacher.join(TIMEOUT_MILLIS);
        assertFalse("detachSubscriptionAndClear waits for a drain", detacher.isAlive());

        assertEquals(List.of("A", "B"), symbols(queue.drain(0, 0)));
        assertEquals(List.of(), queue.drain(0, 0));
    }

    @Test
    public void drainReturnsItsEventsWhenACloseTakesThePendingTasks() throws Exception {
        final DXFeedSubscription<Quote> subscription = endpoint.getFeed().createSubscription(Quote.class);
        final EventQueue queue = EventQueue.create(subscription);
        subscription.addSymbols("A");
        publisher.publishEvents(List.of(quote("A", 1)));
        awaitPendingTask(queue);

        // Run by the drain after the task that retrieves A: the close takes the task after it.
        subscription.getExecutor().execute(queue::close);
        subscription.getExecutor().execute(() -> {});

        assertEquals(List.of("A"), symbols(queue.drain(0, 0)));
        assertEquals(0, queue.getDroppedEvents());
        assertNull(queue.drain(0, 0));
    }

    @Test
    public void drainReturnsItsEventsWhenADetachTakesThePendingTasks() throws Exception {
        final DXFeedSubscription<Quote> subscription = endpoint.getFeed().createSubscription(Quote.class);
        final EventQueue queue = EventQueue.create(subscription);
        subscription.addSymbols("A");
        publisher.publishEvents(List.of(quote("A", 1)));
        awaitPendingTask(queue);

        subscription.getExecutor().execute(
                () -> EventQueue.detachSubscriptionAndClear(endpoint.getFeed(), subscription));
        subscription.getExecutor().execute(() -> {});

        assertEquals(List.of("A"), symbols(queue.drain(0, 0)));
        assertEquals(0, queue.getDroppedEvents());
    }

    @Test
    public void failedListenerFailsTheDrainAndCountsItsEvents() throws InterruptedException {
        final DXFeedSubscription<Quote> subscription = endpoint.getFeed().createSubscription(Quote.class);
        final EventQueue queue = EventQueue.create(subscription);
        subscription.addEventListener(events -> {
            throw new UnsupportedOperationException("listener");
        });
        subscription.addSymbols("A", "B");
        publisher.publishEvents(List.of(quote("A", 1), quote("B", 2)));

        assertThrows(UnsupportedOperationException.class, () -> queue.drain(TIMEOUT_MILLIS, 0));
        assertEquals(2, queue.getDroppedEvents());
    }

    @Test
    public void timeoutBeyondNanosecondsWaitsUntilTheWakeUp() throws Exception {
        final EventQueue queue = EventQueue.create(endpoint.getFeed().createSubscription(Quote.class));

        final AtomicReference<Object> result = new AtomicReference<>("not finished");
        final Thread drainer = startDrain(queue, Long.MAX_VALUE, result);
        awaitDrainInProgress(queue);
        Thread.sleep(200);
        assertTrue("a drain with Long.MAX_VALUE returned at once", drainer.isAlive());

        queue.wakeUp();
        drainer.join(TIMEOUT_MILLIS);
        assertFalse(drainer.isAlive());
        assertEquals(List.of(), result.get());
    }

    @Test
    public void endpointCloseWaitsForTheDrainOfAnOpenQueue() throws Exception {
        // The documented behaviour (dxfg_v4_event_queue.h): closing an endpoint does not close its queues, and
        // closeAndAwaitTermination waits until the undrained events of an open queue are drained.
        final DXFeedSubscription<Quote> subscription = endpoint.getFeed().createSubscription(Quote.class);
        final EventQueue queue = EventQueue.create(subscription);
        subscription.addSymbols("A");
        publisher.publishEvents(List.of(quote("A", 1)));
        awaitPendingTask(queue);

        final Thread closer = start(() -> {
            try {
                endpoint.closeAndAwaitTermination();
            } catch (final InterruptedException e) {
                throw new AssertionError(e);
            }
        });
        closer.join(500);
        assertTrue("closeAndAwaitTermination returned with an undrained queue", closer.isAlive());
        assertFalse(queue.isClosed());

        assertNotNull(queue.drain(0, 0));
        closer.join(TIMEOUT_MILLIS);
        assertFalse("closeAndAwaitTermination hangs after the drain", closer.isAlive());
    }

    private void assertClosedWakesTheDrain(final EventQueue queue, final Runnable close) throws Exception {
        final AtomicReference<Object> result = new AtomicReference<>("not finished");
        final Thread drainer = startDrain(queue, -1, result);
        awaitDrainInProgress(queue);
        close.run();
        drainer.join(TIMEOUT_MILLIS);
        assertFalse(drainer.isAlive());
        assertNull(result.get());

        final long start = System.nanoTime();
        assertNull(queue.drain(TIMEOUT_MILLIS, 0));
        assertTrue(elapsedMillis(start) < 1_000);
    }

    private static Thread startDrain(final EventQueue queue, final long timeoutMillis,
            final AtomicReference<Object> result) {
        return start(() -> {
            try {
                result.set(queue.drain(timeoutMillis, 0));
            } catch (final InterruptedException e) {
                result.set(e);
            }
        });
    }

    private static void awaitDrainInProgress(final EventQueue queue) throws Exception {
        final Field drainThread = EventQueue.class.getDeclaredField("drainThread");
        final Field lock = EventQueue.class.getDeclaredField("lock");
        drainThread.setAccessible(true);
        lock.setAccessible(true);
        awaitCondition(() -> {
            synchronized (getUnchecked(lock, queue)) {
                return getUnchecked(drainThread, queue) != null;
            }
        });
    }

    // Waits until QD has posted a task to the queue.
    private static void awaitPendingTask(final EventQueue queue) throws Exception {
        final Field tasks = EventQueue.class.getDeclaredField("tasks");
        final Field lock = EventQueue.class.getDeclaredField("lock");
        tasks.setAccessible(true);
        lock.setAccessible(true);
        awaitCondition(() -> {
            synchronized (getUnchecked(lock, queue)) {
                return !((Collection<?>) getUnchecked(tasks, queue)).isEmpty();
            }
        });
    }

    private static Object getUnchecked(final Field field, final Object target) {
        try {
            return field.get(target);
        } catch (final IllegalAccessException e) {
            throw new AssertionError(e);
        }
    }

    // Drains until a drain with a short timeout returns no events.
    private static List<EventType<?>> drainAll(final EventQueue queue, final int limit) throws InterruptedException {
        final List<EventType<?>> all = new ArrayList<>();
        List<EventType<?>> events;
        while (!(events = queue.drain(200, limit)).isEmpty()) {
            all.addAll(events);
        }
        return all;
    }

    private void publishTimeAndSales(final String symbol, final int count) {
        final List<TimeAndSale> batch = new ArrayList<>(1_000);
        for (int i = 0; i < count; i++) {
            final TimeAndSale event = new TimeAndSale(symbol);
            event.setPrice(i);
            batch.add(event);
            if (batch.size() == 1_000) {
                publisher.publishEvents(batch);
                batch.clear();
            }
        }
        publisher.publishEvents(batch);
    }

    private static void assertAscendingPrices(final List<EventType<?>> events) {
        double previous = -1;
        for (final EventType<?> event : events) {
            final double price = ((TimeAndSale) event).getPrice();
            assertTrue(previous + " then " + price, price > previous);
            previous = price;
        }
    }

    private static Quote quote(final String symbol, final double bidPrice) {
        final Quote quote = new Quote(symbol);
        quote.setBidPrice(bidPrice);
        return quote;
    }

    private static List<String> symbols(final List<EventType<?>> events) {
        assertNotNull(events);
        final List<String> symbols = new ArrayList<>();
        for (final EventType<?> event : events) {
            symbols.add(String.valueOf(event.getEventSymbol()));
        }
        return symbols;
    }

    private static Thread start(final Runnable task) {
        final Thread thread = new Thread(task);
        thread.setDaemon(true);
        thread.start();
        return thread;
    }

    private static void awaitCondition(final Condition condition) throws Exception {
        final long deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(TIMEOUT_MILLIS);
        while (!condition.test()) {
            assertTrue("condition not met", System.nanoTime() < deadline);
            Thread.sleep(5);
        }
    }

    private static long elapsedMillis(final long start) {
        return TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);
    }

    private interface Condition {
        boolean test() throws Exception;
    }
}
