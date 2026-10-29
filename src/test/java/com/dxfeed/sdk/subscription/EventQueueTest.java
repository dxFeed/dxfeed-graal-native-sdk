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
import com.dxfeed.api.DXFeedEventListener;
import com.dxfeed.api.DXFeedSubscription;
import com.dxfeed.api.DXPublisher;
import com.dxfeed.api.osub.ObservableSubscriptionChangeListener;
import com.dxfeed.event.EventType;
import com.dxfeed.event.market.Quote;
import com.dxfeed.event.market.TimeAndSale;
import com.dxfeed.ipf.InstrumentProfile;
import com.dxfeed.ipf.live.InstrumentProfileCollector;
import com.dxfeed.sdk.javac.NativeListener;
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
import java.util.concurrent.atomic.AtomicInteger;
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
    public void pollReturnsThePublishedEventsInOrder() throws InterruptedException {
        final DXFeedSubscription<Quote> subscription = endpoint.getFeed().createSubscription(Quote.class);
        final EventQueue queue = EventQueue.create(subscription);
        subscription.addSymbols("A", "B", "C");

        publisher.publishEvents(List.of(quote("A", 1), quote("B", 2), quote("C", 3)));

        final List<EventType<?>> events = queue.poll(TIMEOUT_MILLIS, 0);
        assertEquals(List.of("A", "B", "C"), symbols(events));
        assertEquals(0, queue.getDroppedEvents());
    }

    @Test
    public void lastingEventsAreConflatedUntilThePoll() throws InterruptedException {
        final DXFeedSubscription<Quote> subscription = endpoint.getFeed().createSubscription(Quote.class);
        final EventQueue queue = EventQueue.create(subscription);
        subscription.addSymbols("A");

        for (int i = 1; i <= 10_000; i++) {
            publisher.publishEvents(List.of(quote("A", i)));
        }

        final List<EventType<?>> events = pollAll(queue, 0);
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

        final List<EventType<?>> events = pollAll(queue, 0);
        assertTrue("dropped: " + queue.getDroppedEvents(), queue.getDroppedEvents() >= total - BUFFER_SIZE);
        assertEquals(total, events.size() + queue.getDroppedEvents());
        assertAscendingPrices(events);
    }

    @Test
    public void pollReturnsNoEventsAfterTheTimeout() throws InterruptedException {
        final EventQueue queue = EventQueue.create(endpoint.getFeed().createSubscription(Quote.class));

        long start = System.nanoTime();
        assertEquals(List.of(), queue.poll(50, 0));
        final long waited = elapsedMillis(start);
        assertTrue("waited " + waited, waited >= 45 && waited < 5_000);

        start = System.nanoTime();
        assertEquals(List.of(), queue.poll(0, 0));
        assertTrue(elapsedMillis(start) < 1_000);
    }

    @Test
    public void wakeUpEndsTheNextOrTheCurrentPoll() throws Exception {
        final EventQueue queue = EventQueue.create(endpoint.getFeed().createSubscription(Quote.class));

        queue.wakeUp();
        long start = System.nanoTime();
        assertEquals(List.of(), queue.poll(TIMEOUT_MILLIS, 0));
        assertTrue(elapsedMillis(start) < 1_000);

        final AtomicReference<Object> result = new AtomicReference<>();
        final Thread poller = startPoll(queue, TIMEOUT_MILLIS, result);
        awaitPollInProgress(queue);
        start = System.nanoTime();
        queue.wakeUp();
        poller.join(TIMEOUT_MILLIS);
        assertFalse(poller.isAlive());
        assertTrue("woken after " + elapsedMillis(start), elapsedMillis(start) < 500);
        assertEquals(List.of(), result.get());
    }

    @Test
    public void subscriptionCloseEndsThePollWithClosed() throws Exception {
        final DXFeedSubscription<Quote> subscription = endpoint.getFeed().createSubscription(Quote.class);
        final EventQueue queue = EventQueue.create(subscription);
        assertClosedWakesThePoll(queue, subscription::close);
    }

    @Test
    public void queueCloseEndsThePollWithClosed() throws Exception {
        final EventQueue queue = EventQueue.create(endpoint.getFeed().createSubscription(Quote.class));
        assertClosedWakesThePoll(queue, queue::close);
    }

    @Test
    public void closeAllEndsThePollWithClosedAndClosesTheQueuesCreatedLater() throws Exception {
        final EventQueue queue = EventQueue.create(endpoint.getFeed().createSubscription(Quote.class));
        assertClosedWakesThePoll(queue, EventQueue::closeAll);

        final EventQueue later = EventQueue.create(endpoint.getFeed().createSubscription(Quote.class));
        assertTrue(later.isClosed());
        assertNull(later.poll(TIMEOUT_MILLIS, 0));
    }

    @Test
    public void queueOfAClosedSubscriptionIsClosed() throws Exception {
        final DXFeedSubscription<Quote> subscription = endpoint.getFeed().createSubscription(Quote.class);
        subscription.close();
        final EventQueue queue = EventQueue.create(subscription);

        assertTrue(queue.isClosed());
        assertNull(queue.poll(TIMEOUT_MILLIS, 0));
    }

    @Test
    public void closeAllClosesEveryQueueWhenTheirTasksFail() {
        final EventQueue first = EventQueue.create();
        final EventQueue second = EventQueue.create();
        for (final EventQueue queue : List.of(first, second)) {
            queue.getExecutor().execute(() -> {
                throw new IllegalStateException("The task failed");
            });
        }

        final IllegalStateException failure = assertThrows(IllegalStateException.class, EventQueue::closeAll);
        assertEquals(1, failure.getSuppressed().length);
        assertTrue(first.isClosed());
        assertTrue(second.isClosed());
    }

    @Test
    public void concurrentPollsDoNotSplitABatch() throws Exception {
        final DXFeedSubscription<TimeAndSale> subscription = endpoint.getFeed().createSubscription(TimeAndSale.class);
        final EventQueue queue = EventQueue.create(subscription);
        subscription.addSymbols("A");

        final int total = 100_000;
        final ConcurrentLinkedQueue<List<EventType<?>>> batches = new ConcurrentLinkedQueue<>();
        final List<Thread> pollers = new ArrayList<>();
        for (int i = 0; i < 2; i++) {
            pollers.add(start(() -> {
                try {
                    List<EventType<?>> events;
                    while ((events = queue.poll(TIMEOUT_MILLIS, 1_000)) != null) {
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
        for (final Thread poller : pollers) {
            poller.join(TIMEOUT_MILLIS);
            assertFalse(poller.isAlive());
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
    public void pushListenersRunOnThePollingThread() throws InterruptedException {
        final DXFeedSubscription<Quote> subscription = endpoint.getFeed().createSubscription(Quote.class);
        final EventQueue queue = EventQueue.create(subscription);
        final Set<Thread> listenerThreads = Collections.synchronizedSet(new HashSet<>());
        subscription.addEventListener(events -> listenerThreads.add(Thread.currentThread()));
        subscription.addSymbols("A");

        publisher.publishEvents(List.of(quote("A", 1)));

        assertEquals(1, queue.poll(TIMEOUT_MILLIS, 0).size());
        assertEquals(Set.of(Thread.currentThread()), listenerThreads);
    }

    @Test
    public void misuseFailsWithIllegalStateException() {
        final DXFeedSubscription<Quote> withSymbols = endpoint.getFeed().createSubscription(Quote.class);
        withSymbols.addSymbols("A");
        // QD refuses a listener on a subscription attached to a feed that has symbols.
        assertThrows(IllegalStateException.class, () -> EventQueue.create(withSymbols));
        assertNull(EventQueue.of(withSymbols));
        assertNull(withSymbols.getExecutor());

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
    public void pollFromAListenerOfItsOwnPollFails() throws InterruptedException {
        final DXFeedSubscription<Quote> subscription = endpoint.getFeed().createSubscription(Quote.class);
        final EventQueue queue = EventQueue.create(subscription);
        final AtomicReference<Throwable> failure = new AtomicReference<>();
        subscription.addEventListener(events -> {
            try {
                queue.poll(0, 0);
            } catch (final Throwable e) {
                failure.set(e);
            }
        });
        subscription.addSymbols("A");

        publisher.publishEvents(List.of(quote("A", 1)));

        assertEquals(1, queue.poll(TIMEOUT_MILLIS, 0).size());
        assertTrue(String.valueOf(failure.get()), failure.get() instanceof IllegalStateException);
    }

    @Test
    public void droppedBatchIsCountedOnce() throws InterruptedException {
        final DXFeedSubscription<Quote> subscription = endpoint.getFeed().createSubscription(Quote.class);
        final EventQueue queue = EventQueue.create(subscription);
        subscription.addSymbols("A", "B");

        publisher.publishEvents(List.of(quote("A", 1), quote("B", 2)));
        queue.dropBatch(queue.poll(TIMEOUT_MILLIS, 0));
        assertEquals(2, queue.getDroppedEvents());

        publisher.publishEvents(List.of(quote("A", 3)));
        assertEquals(1, queue.poll(TIMEOUT_MILLIS, 0).size());
        assertEquals(2, queue.getDroppedEvents());
    }

    @Test
    public void closeWithPendingDataCountsItAndLetsTheEndpointClose() throws Exception {
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
    public void detachAndClearReturnsWithoutAPoll() throws Exception {
        final DXFeedSubscription<Quote> subscription = endpoint.getFeed().createSubscription(Quote.class);
        final EventQueue queue = EventQueue.create(subscription);
        subscription.addSymbols("A", "B");
        publisher.publishEvents(List.of(quote("A", 1), quote("B", 2)));

        final Thread detacher = start(() -> EventQueue.detachSubscriptionAndClear(endpoint.getFeed(), subscription));
        detacher.join(TIMEOUT_MILLIS);
        assertFalse("detachSubscriptionAndClear waits for a poll", detacher.isAlive());

        assertEquals(List.of("A", "B"), symbols(queue.poll(0, 0)));
        assertEquals(List.of(), queue.poll(0, 0));
    }

    @Test
    public void pollReturnsItsEventsWhenACloseTakesThePendingTasks() throws Exception {
        final DXFeedSubscription<Quote> subscription = endpoint.getFeed().createSubscription(Quote.class);
        final EventQueue queue = EventQueue.create(subscription);
        subscription.addSymbols("A");
        publisher.publishEvents(List.of(quote("A", 1)));
        awaitPendingTask(queue);

        // Run by the poll after the task that retrieves A: the close takes the task after it.
        subscription.getExecutor().execute(queue::close);
        subscription.getExecutor().execute(() -> {});

        assertEquals(List.of("A"), symbols(queue.poll(0, 0)));
        assertEquals(0, queue.getDroppedEvents());
        assertNull(queue.poll(0, 0));
    }

    @Test
    public void pollReturnsItsEventsWhenADetachTakesThePendingTasks() throws Exception {
        final DXFeedSubscription<Quote> subscription = endpoint.getFeed().createSubscription(Quote.class);
        final EventQueue queue = EventQueue.create(subscription);
        subscription.addSymbols("A");
        publisher.publishEvents(List.of(quote("A", 1)));
        awaitPendingTask(queue);

        subscription.getExecutor().execute(
                () -> EventQueue.detachSubscriptionAndClear(endpoint.getFeed(), subscription));
        subscription.getExecutor().execute(() -> {});

        assertEquals(List.of("A"), symbols(queue.poll(0, 0)));
        assertEquals(0, queue.getDroppedEvents());
    }

    @Test
    public void failedListenerFailsThePollAndCountsItsEvents() throws InterruptedException {
        final DXFeedSubscription<Quote> subscription = endpoint.getFeed().createSubscription(Quote.class);
        final EventQueue queue = EventQueue.create(subscription);
        subscription.addEventListener(events -> {
            throw new UnsupportedOperationException("listener");
        });
        subscription.addSymbols("A", "B");
        publisher.publishEvents(List.of(quote("A", 1), quote("B", 2)));

        assertThrows(UnsupportedOperationException.class, () -> queue.poll(TIMEOUT_MILLIS, 0));
        assertEquals(2, queue.getDroppedEvents());
    }

    @Test
    public void timeoutBeyondNanosecondsWaitsUntilTheWakeUp() throws Exception {
        final EventQueue queue = EventQueue.create(endpoint.getFeed().createSubscription(Quote.class));

        final AtomicReference<Object> result = new AtomicReference<>("not finished");
        final Thread poller = startPoll(queue, Long.MAX_VALUE, result);
        awaitPollInProgress(queue);
        Thread.sleep(200);
        assertTrue("a poll with Long.MAX_VALUE returned at once", poller.isAlive());

        queue.wakeUp();
        poller.join(TIMEOUT_MILLIS);
        assertFalse(poller.isAlive());
        assertEquals(List.of(), result.get());
    }

    @Test
    public void endpointCloseWaitsForThePollOfAnOpenQueue() throws Exception {
        // The documented behaviour (dxfg_v4_event_queue.h): closing an endpoint does not close its queues, and
        // closeAndAwaitTermination waits until a poll returns the pending events of an open queue.
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
        assertTrue("closeAndAwaitTermination returned with events pending in an open queue", closer.isAlive());
        assertFalse(queue.isClosed());

        assertNotNull(queue.poll(0, 0));
        closer.join(TIMEOUT_MILLIS);
        assertFalse("closeAndAwaitTermination hangs after the poll", closer.isAlive());
    }

    // The shared queue: attachments, tags and the queue-level executor.

    @Test
    public void attachedSubscriptionsDeliverTheirEventsWithTheirTags() throws InterruptedException {
        final EventQueue queue = EventQueue.create();
        final DXFeedSubscription<TimeAndSale> first = endpoint.getFeed().createSubscription(TimeAndSale.class);
        final DXFeedSubscription<TimeAndSale> second = endpoint.getFeed().createSubscription(TimeAndSale.class);
        queue.attach(first, 1);
        queue.attach(second, 2);
        assertSame(queue, EventQueue.of(first));
        assertSame(queue, EventQueue.of(second));
        first.addSymbols("A");
        second.addSymbols("A");
        publishTimeAndSales("A", 3_000);

        final List<EventType<?>> firstEvents = new ArrayList<>();
        final List<EventType<?>> secondEvents = new ArrayList<>();
        EventQueue.Batch batch;
        while (!(batch = queue.pollTagged(200, 500)).isEmpty()) {
            for (int i = 0; i < batch.size(); i++) {
                assertTrue("tag " + batch.tag(i), batch.tag(i) == 1 || batch.tag(i) == 2);
                (batch.tag(i) == 1 ? firstEvents : secondEvents).add(batch.events().get(i));
            }
        }
        assertEquals(3_000, firstEvents.size());
        assertEquals(3_000, secondEvents.size());
        assertAscendingPrices(firstEvents);
        assertAscendingPrices(secondEvents);
        assertEquals(0, queue.getDroppedEvents());
    }

    @Test
    public void plainPollOfASharedQueueReturnsTheEventsWithoutTags() throws InterruptedException {
        final EventQueue queue = EventQueue.create();
        final DXFeedSubscription<Quote> subscription = endpoint.getFeed().createSubscription(Quote.class);
        queue.attach(subscription, 7);
        subscription.addSymbols("A");
        publisher.publishEvents(List.of(quote("A", 1)));

        assertEquals(List.of("A"), symbols(queue.poll(TIMEOUT_MILLIS, 0)));
    }

    @Test
    public void boundQueueTagsItsEventsWithZero() throws InterruptedException {
        final DXFeedSubscription<Quote> subscription = endpoint.getFeed().createSubscription(Quote.class);
        final EventQueue queue = EventQueue.create(subscription);
        subscription.addSymbols("A");
        publisher.publishEvents(List.of(quote("A", 1)));

        final EventQueue.Batch batch = queue.pollTagged(TIMEOUT_MILLIS, 0);
        assertEquals(1, batch.size());
        assertEquals(0, batch.tag(0));
    }

    @Test
    public void queueWithoutAttachmentsWaitsForALaterAttachment() throws Exception {
        final EventQueue queue = EventQueue.create();
        assertEquals(List.of(), queue.poll(0, 0));

        final AtomicReference<Object> result = new AtomicReference<>("not finished");
        final Thread poller = startPoll(queue, -1, result);
        awaitPollInProgress(queue);
        final DXFeedSubscription<Quote> subscription = endpoint.getFeed().createSubscription(Quote.class);
        queue.attach(subscription, 1);
        subscription.addSymbols("A");
        publisher.publishEvents(List.of(quote("A", 1)));
        poller.join(TIMEOUT_MILLIS);
        assertFalse(poller.isAlive());
        assertEquals(List.of("A"), symbols(castEvents(result.get())));
    }

    @Test
    public void attachMisuseFailsWithIllegalStateException() {
        final EventQueue queue = EventQueue.create();
        final EventQueue other = EventQueue.create();

        final DXFeedSubscription<Quote> withSymbols = endpoint.getFeed().createSubscription(Quote.class);
        withSymbols.addSymbols("A");
        assertThrows(IllegalStateException.class, () -> queue.attach(withSymbols, 1));

        final DXFeedSubscription<Quote> withExecutor = endpoint.getFeed().createSubscription(Quote.class);
        withExecutor.setExecutor(Runnable::run);
        assertThrows(IllegalStateException.class, () -> queue.attach(withExecutor, 1));

        final DXFeedSubscription<Quote> attached = endpoint.getFeed().createSubscription(Quote.class);
        queue.attach(attached, 1);
        assertThrows(IllegalStateException.class, () -> queue.attach(attached, 2));
        assertThrows(IllegalStateException.class, () -> other.attach(attached, 2));

        final DXFeedSubscription<Quote> boundSubscription = endpoint.getFeed().createSubscription(Quote.class);
        final EventQueue bound = EventQueue.create(boundSubscription);
        assertThrows(IllegalStateException.class,
                () -> bound.attach(endpoint.getFeed().createSubscription(Quote.class), 1));
        assertThrows(IllegalStateException.class, bound::getExecutor);

        // A subscription that has the queue-level executor has an executor.
        final DXFeedSubscription<Quote> onQueueExecutor = endpoint.getFeed().createSubscription(Quote.class);
        onQueueExecutor.setExecutor(queue.getExecutor());
        assertThrows(IllegalStateException.class, () -> other.attach(onQueueExecutor, 1));
    }

    @Test
    public void detachedSubscriptionWithSymbolsCanBeAttached() throws InterruptedException {
        final DXFeedSubscription<Quote> forBound = new DXFeedSubscription<>(Quote.class);
        forBound.addSymbols("A");
        final EventQueue bound = EventQueue.create(forBound);
        final DXFeedSubscription<Quote> forShared = new DXFeedSubscription<>(Quote.class);
        forShared.addSymbols("B");
        final EventQueue shared = EventQueue.create();
        shared.attach(forShared, 7);

        endpoint.getFeed().attachSubscription(forBound);
        endpoint.getFeed().attachSubscription(forShared);
        publisher.publishEvents(List.of(quote("A", 1), quote("B", 2)));

        assertEquals(List.of("A"), symbols(bound.poll(TIMEOUT_MILLIS, 0)));
        final EventQueue.Batch batch = shared.pollTagged(TIMEOUT_MILLIS, 0);
        assertEquals(List.of("B"), symbols(batch.events()));
        assertEquals(7, batch.tag(0));
    }

    @Test
    public void failedAttachLeavesTheSubscriptionWithoutExecutor() {
        final EventQueue queue = EventQueue.create();
        final AtomicInteger listeners = new AtomicInteger();
        final DXFeedSubscription<Quote> subscription = new DXFeedSubscription<>(Quote.class) {
            @Override
            public synchronized void addEventListener(final DXFeedEventListener<Quote> listener) {
                super.addEventListener(listener);
                listeners.incrementAndGet();
            }

            @Override
            public synchronized void removeEventListener(final DXFeedEventListener<Quote> listener) {
                super.removeEventListener(listener);
                listeners.decrementAndGet();
            }

            @Override
            public synchronized void addChangeListener(final ObservableSubscriptionChangeListener listener) {
                throw new UnsupportedOperationException("addChangeListener");
            }
        };
        assertThrows(UnsupportedOperationException.class, () -> queue.attach(subscription, 1));
        assertNull(subscription.getExecutor());
        assertNull(EventQueue.of(subscription));
        assertEquals(0, listeners.get());
    }

    @Test
    public void concurrentAttachesOfOneSubscriptionLetOneWin() throws Exception {
        for (int round = 0; round < 50; round++) {
            final EventQueue first = EventQueue.create();
            final EventQueue second = EventQueue.create();
            final DXFeedSubscription<Quote> subscription = endpoint.getFeed().createSubscription(Quote.class);
            final AtomicInteger failures = new AtomicInteger();
            final Thread one = start(() -> attachCounting(first, subscription, failures));
            final Thread two = start(() -> attachCounting(second, subscription, failures));
            one.join(TIMEOUT_MILLIS);
            two.join(TIMEOUT_MILLIS);
            assertEquals(1, failures.get());
            assertNotNull(EventQueue.of(subscription));
        }
    }

    @Test
    public void closedSubscriptionOrQueueIsAttachedAsDetached() throws Exception {
        final EventQueue queue = EventQueue.create();
        final DXFeedSubscription<Quote> closedSubscription = endpoint.getFeed().createSubscription(Quote.class);
        closedSubscription.close();
        queue.attach(closedSubscription, 1);

        final EventQueue closedQueue = EventQueue.create();
        closedQueue.close();
        final DXFeedSubscription<Quote> subscription = endpoint.getFeed().createSubscription(Quote.class);
        closedQueue.attach(subscription, 1);
        subscription.addSymbols("A");
        publisher.publishEvents(List.of(quote("A", 1)));

        assertEquals(1, closedQueue.getDroppedEvents());
        assertTrue(pendingTasks(closedQueue).isEmpty());
        assertNull(closedQueue.poll(0, 0));
        assertTrue(pendingTasks(queue).isEmpty());
    }

    @Test
    public void subscriptionCloseKeepsTheSharedPollOpen() throws Exception {
        final EventQueue queue = EventQueue.create();
        final DXFeedSubscription<Quote> first = endpoint.getFeed().createSubscription(Quote.class);
        final DXFeedSubscription<Quote> second = endpoint.getFeed().createSubscription(Quote.class);
        queue.attach(first, 1);
        queue.attach(second, 2);
        first.addSymbols("A");
        second.addSymbols("B");
        publisher.publishEvents(List.of(quote("A", 1)));
        assertEquals(List.of("A"), symbols(queue.poll(TIMEOUT_MILLIS, 0)));

        final AtomicReference<Object> result = new AtomicReference<>("not finished");
        final Thread poller = start(() -> {
            try {
                result.set(queue.pollTagged(-1, 0));
            } catch (final InterruptedException e) {
                result.set(e);
            }
        });
        awaitPollInProgress(queue);
        final long droppedBefore = queue.getDroppedEvents();
        first.close();
        poller.join(200);
        assertTrue("a subscription close ended the poll of a shared queue", poller.isAlive());
        assertTrue(queue.getDroppedEvents() >= droppedBefore);

        publisher.publishEvents(List.of(quote("A", 2), quote("B", 3)));
        poller.join(TIMEOUT_MILLIS);
        assertFalse(poller.isAlive());
        final EventQueue.Batch batch = (EventQueue.Batch) result.get();
        assertEquals(List.of("B"), symbols(batch.events()));
        assertEquals(2, batch.tag(0));

        final Thread closedPoller = startPoll(queue, -1, result);
        awaitPollInProgress(queue);
        queue.close();
        closedPoller.join(TIMEOUT_MILLIS);
        assertFalse(closedPoller.isAlive());
        assertNull(result.get());
    }

    @Test
    public void detachAndClearRunsOnlyTheTasksOfItsSubscription() throws Exception {
        final EventQueue queue = EventQueue.create();
        final DXFeedSubscription<Quote> first = endpoint.getFeed().createSubscription(Quote.class);
        final DXFeedSubscription<Quote> second = endpoint.getFeed().createSubscription(Quote.class);
        queue.attach(first, 1);
        queue.attach(second, 2);
        first.addSymbols("A");
        second.addSymbols("B");
        publisher.publishEvents(List.of(quote("A", 1), quote("B", 2)));
        awaitCondition(() -> pendingTasks(queue).size() >= 2);
        final AtomicReference<Thread> queueTaskThread = new AtomicReference<>();
        queue.getExecutor().execute(() -> queueTaskThread.set(Thread.currentThread()));

        final Thread detacher = start(() -> EventQueue.detachSubscriptionAndClear(endpoint.getFeed(), first));
        detacher.join(TIMEOUT_MILLIS);
        assertFalse(detacher.isAlive());
        assertNull("a task of another owner ran on the detaching thread", queueTaskThread.get());
        assertEquals(2, pendingTasks(queue).size());

        final EventQueue.Batch batch = queue.pollTagged(0, 0);
        assertEquals(List.of("A", "B"), symbols(batch.events()));
        assertEquals(1, batch.tag(0));
        assertEquals(2, batch.tag(1));
        assertSame(Thread.currentThread(), queueTaskThread.get());
    }

    @Test
    public void detachAndClearDuringAPollKeepsTheOrderOfItsSubscription() throws Exception {
        final EventQueue queue = EventQueue.create();
        final DXFeedSubscription<TimeAndSale> subscription = endpoint.getFeed().createSubscription(TimeAndSale.class);
        queue.attach(subscription, 1);
        subscription.addSymbols("A");
        publishTimeAndSales("A", 100_000);

        final Thread detacher = start(() -> {
            try {
                Thread.sleep(5);
            } catch (final InterruptedException e) {
                throw new AssertionError(e);
            }
            EventQueue.detachSubscriptionAndClear(endpoint.getFeed(), subscription);
        });
        final List<EventType<?>> events = new ArrayList<>();
        List<EventType<?>> batch;
        while (!(batch = queue.poll(200, 100)).isEmpty() || detacher.isAlive()) {
            events.addAll(batch);
        }
        assertFalse(events.isEmpty());
        assertAscendingPrices(events);
    }

    @Test
    public void queueLevelTasksRunInsideAPollOnThePollingThread() throws Exception {
        final EventQueue queue = EventQueue.create();
        final List<Thread> threads = Collections.synchronizedList(new ArrayList<>());
        final Thread poster = start(() -> queue.getExecutor().execute(() -> threads.add(Thread.currentThread())));
        poster.join(TIMEOUT_MILLIS);
        assertTrue(threads.isEmpty());

        // A poll that ran a queue-level task and has no events returns at once.
        final long start = System.nanoTime();
        assertEquals(List.of(), queue.poll(TIMEOUT_MILLIS, 0));
        assertTrue(elapsedMillis(start) < 1_000);
        assertEquals(List.of(Thread.currentThread()), threads);
    }

    @Test
    public void steadyQueueLevelFlowDoesNotKeepAPollFromReturning() throws Exception {
        final EventQueue queue = EventQueue.create();
        final Executor executor = queue.getExecutor();
        final AtomicInteger runs = new AtomicInteger();
        executor.execute(new Runnable() {
            @Override
            public void run() {
                // Bounded: a closed queue runs a task at once, so an endless repost would recurse.
                if (runs.incrementAndGet() < 1_000) {
                    executor.execute(this);
                }
            }
        });

        final long start = System.nanoTime();
        assertEquals(List.of(), queue.poll(TIMEOUT_MILLIS, 0));
        assertTrue(elapsedMillis(start) < 1_000);
        // The first task and the one it posted, pending when the first one ran.
        assertEquals(2, runs.get());
    }

    @Test
    public void wakeUpFromAQueueLevelTaskIsConsumedByThePoll() throws Exception {
        final EventQueue queue = EventQueue.create();
        queue.getExecutor().execute(queue::wakeUp);

        final long start = System.nanoTime();
        assertEquals(List.of(), queue.poll(TIMEOUT_MILLIS, 0));
        assertTrue(elapsedMillis(start) < 1_000);
        // The wake-up is consumed.
        final long next = System.nanoTime();
        assertEquals(List.of(), queue.poll(100, 0));
        assertTrue(elapsedMillis(next) >= 90);
    }

    @Test
    public void limitOneKeepsTheOrderOfEachTag() throws InterruptedException {
        final EventQueue queue = EventQueue.create();
        final DXFeedSubscription<TimeAndSale> first = endpoint.getFeed().createSubscription(TimeAndSale.class);
        final DXFeedSubscription<TimeAndSale> second = endpoint.getFeed().createSubscription(TimeAndSale.class);
        queue.attach(first, 1);
        queue.attach(second, 2);
        first.addSymbols("A");
        second.addSymbols("B");
        publishTimeAndSales("A", 2_000);
        publishTimeAndSales("B", 2_000);

        final List<EventType<?>> a = new ArrayList<>();
        final List<EventType<?>> b = new ArrayList<>();
        EventQueue.Batch batch;
        while (!(batch = queue.pollTagged(200, 1)).isEmpty()) {
            for (int i = 0; i < batch.size(); i++) {
                (batch.tag(i) == 1 ? a : b).add(batch.events().get(i));
            }
        }
        assertEquals(2_000, a.size());
        assertEquals(2_000, b.size());
        assertAscendingPrices(a);
        assertAscendingPrices(b);
    }

    @Test
    public void handlesOfTheQueueAndOfItsExecutorKeepItOpen() {
        final EventQueue queue = EventQueue.create();
        final Executor executor = queue.getExecutor();
        assertSame(queue, EventQueue.countedBy(queue));
        assertSame(queue, EventQueue.countedBy(executor));

        final DXFeedSubscription<Quote> subscription = endpoint.getFeed().createSubscription(Quote.class);
        queue.attach(subscription, 1);
        assertNull("the executor of an attachment counts", EventQueue.countedBy(subscription.getExecutor()));

        queue.handleCreated(); // the queue handle
        queue.handleCreated(); // the executor handle
        queue.handleCreated(); // its clone
        queue.handleReleased();
        queue.handleReleased();
        assertFalse(queue.isClosed());
        queue.handleReleased();
        assertTrue(queue.isClosed());
    }

    @Test
    public void closeRunsThePendingTasksMutedAndLaterOnesAtOnce() throws Exception {
        final EventQueue queue = EventQueue.create();
        final TestListener listener = new TestListener();
        queue.getExecutor().execute(listener::call);
        final DXFeedSubscription<Quote> subscription = endpoint.getFeed().createSubscription(Quote.class);
        queue.attach(subscription, 1);
        subscription.addSymbols("A");
        publisher.publishEvents(List.of(quote("A", 1)));
        awaitCondition(() -> pendingTasks(queue).size() == 2);

        queue.close();
        assertTrue(pendingTasks(queue).isEmpty());
        assertEquals(0, listener.calls.get());
        assertEquals(1, queue.getDroppedEvents());

        final AtomicReference<Thread> runner = new AtomicReference<>();
        queue.getExecutor().execute(() -> {
            runner.set(Thread.currentThread());
            listener.call();
        });
        assertSame(Thread.currentThread(), runner.get());
        assertEquals(0, listener.calls.get());

        // Outside the tasks of a closed queue the listener is called.
        listener.call();
        assertEquals(1, listener.calls.get());
    }

    @Test
    public void lastHandleReleaseMutesTheQueueLevelTasks() {
        final EventQueue queue = EventQueue.create();
        final TestListener listener = new TestListener();
        queue.handleCreated();
        queue.getExecutor().execute(listener::call);
        queue.handleReleased();
        assertTrue(queue.isClosed());
        assertEquals(0, listener.calls.get());
    }

    @Test
    public void subscriptionOnTheQueueExecutorDeliversInAPoll() throws Exception {
        final EventQueue queue = EventQueue.create();
        final DXFeedSubscription<Quote> subscription = endpoint.getFeed().createSubscription(Quote.class);
        EventQueue.setExecutor(subscription::getExecutor, subscription::setExecutor, queue.getExecutor());
        final List<Thread> threads = Collections.synchronizedList(new ArrayList<>());
        subscription.addEventListener(events -> threads.add(Thread.currentThread()));
        subscription.addSymbols("A");
        publisher.publishEvents(List.of(quote("A", 1)));
        awaitCondition(() -> !pendingTasks(queue).isEmpty());
        assertTrue(threads.isEmpty());
        assertEquals(List.of(), queue.poll(TIMEOUT_MILLIS, 0));
        assertEquals(List.of(Thread.currentThread()), threads);

        // detachSubscriptionAndClear waits until a poll runs the queue-level task (a documented limit).
        publisher.publishEvents(List.of(quote("A", 2)));
        awaitCondition(() -> !pendingTasks(queue).isEmpty());
        final Thread detacher = start(() -> EventQueue.detachSubscriptionAndClear(endpoint.getFeed(), subscription));
        detacher.join(200);
        assertTrue("detachSubscriptionAndClear returned before the poll", detacher.isAlive());
        final long deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(TIMEOUT_MILLIS);
        while (detacher.isAlive() && System.nanoTime() < deadline) {
            queue.poll(50, 0);
        }
        assertFalse(detacher.isAlive());

        // The subscription may change it later and back.
        final Executor direct = Runnable::run;
        EventQueue.setExecutor(subscription::getExecutor, subscription::setExecutor, direct);
        EventQueue.setExecutor(subscription::getExecutor, subscription::setExecutor, queue.getExecutor());
        assertSame(queue.getExecutor(), subscription.getExecutor());
    }

    @Test
    public void collectorOnTheQueueExecutorNotifiesInAPoll() throws Exception {
        final EventQueue queue = EventQueue.create();
        final InstrumentProfileCollector collector = new InstrumentProfileCollector();
        collector.setExecutor(queue.getExecutor());
        final List<Thread> threads = Collections.synchronizedList(new ArrayList<>());
        collector.addUpdateListener(profiles -> threads.add(Thread.currentThread()));
        final InstrumentProfile profile = new InstrumentProfile();
        profile.setSymbol("A");
        collector.updateInstrumentProfile(profile);

        awaitCondition(() -> !pendingTasks(queue).isEmpty());
        assertTrue(threads.isEmpty());
        while (!pendingTasks(queue).isEmpty()) {
            queue.poll(0, 0);
        }
        assertFalse(threads.isEmpty());
        for (final Thread thread : threads) {
            assertSame(Thread.currentThread(), thread);
        }
    }

    @Test
    public void executorsAreCheckedByKind() {
        final EventQueue queue = EventQueue.create();
        final DXFeedSubscription<Quote> attached = endpoint.getFeed().createSubscription(Quote.class);
        queue.attach(attached, 1);

        assertThrows(IllegalStateException.class, () -> EventQueue.checkExecutor(attached.getExecutor()));
        assertSame(queue.getExecutor(), EventQueue.checkExecutor(queue.getExecutor()));

        final DXFeedSubscription<Quote> other = endpoint.getFeed().createSubscription(Quote.class);
        assertThrows(IllegalStateException.class,
                () -> EventQueue.setExecutor(other::getExecutor, other::setExecutor, attached.getExecutor()));
        EventQueue.setExecutor(other::getExecutor, other::setExecutor, queue.getExecutor());
        assertSame(queue.getExecutor(), other.getExecutor());
    }

    @Test
    public void closeAllEndsASharedPollAndMutesItsTasks() throws Exception {
        final EventQueue queue = EventQueue.create();
        final DXFeedSubscription<Quote> first = endpoint.getFeed().createSubscription(Quote.class);
        final DXFeedSubscription<Quote> second = endpoint.getFeed().createSubscription(Quote.class);
        queue.attach(first, 1);
        queue.attach(second, 2);
        final TestListener listener = new TestListener();

        final AtomicReference<Object> result = new AtomicReference<>("not finished");
        final Thread poller = startPoll(queue, -1, result);
        awaitPollInProgress(queue);
        EventQueue.closeAll();
        poller.join(TIMEOUT_MILLIS);
        assertFalse(poller.isAlive());
        assertNull(result.get());

        queue.getExecutor().execute(listener::call);
        first.addSymbols("A");
        publisher.publishEvents(List.of(quote("A", 1)));
        assertTrue(pendingTasks(queue).isEmpty());
        assertEquals(0, listener.calls.get());
    }

    private void assertClosedWakesThePoll(final EventQueue queue, final Runnable close) throws Exception {
        final AtomicReference<Object> result = new AtomicReference<>("not finished");
        final Thread poller = startPoll(queue, -1, result);
        awaitPollInProgress(queue);
        close.run();
        poller.join(TIMEOUT_MILLIS);
        assertFalse(poller.isAlive());
        assertNull(result.get());

        final long start = System.nanoTime();
        assertNull(queue.poll(TIMEOUT_MILLIS, 0));
        assertTrue(elapsedMillis(start) < 1_000);
    }

    private static Thread startPoll(final EventQueue queue, final long timeoutMillis,
            final AtomicReference<Object> result) {
        return start(() -> {
            try {
                result.set(queue.poll(timeoutMillis, 0));
            } catch (final InterruptedException e) {
                result.set(e);
            }
        });
    }

    private static void awaitPollInProgress(final EventQueue queue) throws Exception {
        final Field pollThread = EventQueue.class.getDeclaredField("pollThread");
        final Field lock = EventQueue.class.getDeclaredField("lock");
        pollThread.setAccessible(true);
        lock.setAccessible(true);
        awaitCondition(() -> {
            synchronized (getUnchecked(lock, queue)) {
                return getUnchecked(pollThread, queue) != null;
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

    private static void attachCounting(final EventQueue queue, final DXFeedSubscription<?> subscription,
            final AtomicInteger failures) {
        try {
            queue.attach(subscription, 1);
        } catch (final IllegalStateException e) {
            failures.incrementAndGet();
        }
    }

    // A copy of the tasks pending in the queue.
    private static Collection<?> pendingTasks(final EventQueue queue) throws ReflectiveOperationException {
        final Field tasks = EventQueue.class.getDeclaredField("tasks");
        final Field lock = EventQueue.class.getDeclaredField("lock");
        tasks.setAccessible(true);
        lock.setAccessible(true);
        synchronized (lock.get(queue)) {
            return new ArrayList<>((Collection<?>) tasks.get(queue));
        }
    }

    @SuppressWarnings("unchecked")
    private static List<EventType<?>> castEvents(final Object result) {
        return (List<EventType<?>>) result;
    }

    // A native listener without a native function: counts the calls that enter() lets through.
    private static final class TestListener extends NativeListener {
        final AtomicInteger calls = new AtomicInteger();

        void call() {
            if (!enter()) {
                return;
            }
            try {
                calls.incrementAndGet();
            } finally {
                exit();
            }
        }
    }

    private static Object getUnchecked(final Field field, final Object target) {
        try {
            return field.get(target);
        } catch (final IllegalAccessException e) {
            throw new AssertionError(e);
        }
    }

    // Polls until a poll with a short timeout returns no events.
    private static List<EventType<?>> pollAll(final EventQueue queue, final int limit) throws InterruptedException {
        final List<EventType<?>> all = new ArrayList<>();
        List<EventType<?>> events;
        while (!(events = queue.poll(200, limit)).isEmpty()) {
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
