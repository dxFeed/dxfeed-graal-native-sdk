// Copyright (c) 2026 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

package com.dxfeed.sdk.subscription;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import com.dxfeed.api.DXEndpoint;
import com.dxfeed.api.DXFeedEventListener;
import com.dxfeed.api.DXFeedSubscription;
import com.dxfeed.api.DXPublisher;
import com.dxfeed.event.EventType;
import com.dxfeed.event.market.Quote;
import com.dxfeed.sdk.javac.NativeListener;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * The ready signal of {@link EventQueue} (arming, settings, removal and close) with a fake {@link
 * EventQueue.Signaller}. {@link EventQueue#closeAll()} cannot be undone through the API, so the test that calls it
 * undoes it by reflection afterwards.
 */
public class EventQueueReadySignalTest {

    private static final long TIMEOUT_MILLIS = 10_000;
    // How long a thread that must stay blocked is watched.
    private static final long BLOCKED_MILLIS = 200;
    private static final int CONCURRENT_ITERATIONS = 100;
    private static final int PRODUCERS = 4;
    private static final int OPERATIONS_PER_PRODUCER = 250;

    private static final Logger QUEUE_LOGGER = Logger.getLogger(EventQueue.class.getName());

    private DXEndpoint endpoint;
    private DXPublisher publisher;
    private final List<FakeSignaller> fakes = new ArrayList<>();
    private final LogSink logSink = new LogSink();

    @Before
    public void createEndpointAndCaptureLog() {
        endpoint = DXEndpoint.create(DXEndpoint.Role.LOCAL_HUB);
        publisher = endpoint.getPublisher();
        QUEUE_LOGGER.addHandler(logSink);
        QUEUE_LOGGER.setUseParentHandlers(false);
    }

    @After
    public void checkFakesAndCloseEndpoint() throws ReflectiveOperationException, InterruptedException {
        QUEUE_LOGGER.removeHandler(logSink);
        QUEUE_LOGGER.setUseParentHandlers(true);
        logSink.release();
        // A queue left with pending tasks by a failed test would hang closeAndAwaitTermination.
        EventQueue.closeAll();
        undoCloseAll();
        for (final FakeSignaller fake : fakes) {
            assertFalse("a write under the queue's lock", fake.wroteUnderLock.get());
        }
        endpoint.closeAndAwaitTermination();
    }

    @Test
    public void idleQueueSignalsOnceUntilThePollEmptiesItAndRearms() throws Exception {
        final EventQueue queue = EventQueue.create();
        final FakeSignaller fake = set(queue);
        assertEquals(0, fake.calls.get());

        post(queue);
        assertEquals(1, fake.calls.get());
        assertSame(Thread.currentThread(), fake.lastThread);
        post(queue);
        post(queue);
        assertEquals(1, fake.calls.get());

        assertEquals(List.of(), queue.poll(0, 0));
        assertEquals(1, fake.calls.get());
        assertTrue(pendingTasks(queue).isEmpty());

        post(queue);
        assertEquals(2, fake.calls.get());
    }

    @Test
    public void pollStoppedAtItsLimitSignalsFromThePollThreadAfterItsLock() throws Exception {
        final DXFeedSubscription<Quote> first = endpoint.getFeed().createSubscription(Quote.class);
        final DXFeedSubscription<Quote> second = endpoint.getFeed().createSubscription(Quote.class);
        final EventQueue queue = EventQueue.create();
        queue.attach(first, 1);
        queue.attach(second, 2);
        final FakeSignaller fake = set(queue);
        first.addSymbols("A");
        second.addSymbols("B");

        publisher.publishEvents(List.of(quote("A", 1), quote("B", 2)));
        awaitCondition(() -> pendingTasks(queue).size() >= 2);
        assertEquals(1, fake.calls.get());

        final EventQueue.Batch batch = queue.pollTagged(0, 1);
        assertEquals(1, batch.size());
        assertEquals(2, fake.calls.get());
        assertSame(Thread.currentThread(), fake.lastThread);

        // The rest is polled without another signal: the queue is armed again.
        assertEquals(1, queue.poll(0, 0).size());
        assertEquals(2, fake.calls.get());
        assertTrue(pendingTasks(queue).isEmpty());
        post(queue);
        assertEquals(3, fake.calls.get());
    }

    @Test
    public void settingWithPendingWorkSignalsAtOnceOnTheCallingThread() throws Exception {
        final EventQueue queue = EventQueue.create();
        post(queue);
        final FakeSignaller withTask = set(queue);
        assertEquals(1, withTask.calls.get());
        assertSame(Thread.currentThread(), withTask.lastThread);
        post(queue);
        assertEquals(1, withTask.calls.get());
        queue.poll(0, 0);

        final EventQueue woken = EventQueue.create();
        woken.wakeUp();
        final FakeSignaller withWakeUp = set(woken);
        assertEquals(1, withWakeUp.calls.get());
        assertSame(Thread.currentThread(), withWakeUp.lastThread);
        woken.wakeUp();
        assertEquals(1, withWakeUp.calls.get());
        assertEquals(List.of(), woken.poll(TIMEOUT_MILLIS, 0));
    }

    @Test
    public void settingOnAClosedQueueSignalsOnceAndKeepsNothing() throws Exception {
        final EventQueue queue = EventQueue.create();
        queue.close();

        final FakeSignaller fake = set(queue);
        assertEquals(1, fake.calls.get());
        assertSame(Thread.currentThread(), fake.lastThread);
        assertNull(read(queue, "setting"));

        post(queue);
        queue.wakeUp();
        queue.close();
        assertNull(queue.poll(0, 0));
        assertEquals(1, fake.calls.get());

        final FakeSignaller again = set(queue);
        assertEquals(1, again.calls.get());
        assertEquals(1, fake.calls.get());
        queue.setReadySignal(null);
        assertNull(read(queue, "setting"));
    }

    @Test
    public void wakeUpSignalsAndAnOverlappingPollWritesNothingMore() throws Exception {
        final EventQueue queue = EventQueue.create();
        final FakeSignaller fake = set(queue);

        queue.wakeUp();
        assertEquals(1, fake.calls.get());
        queue.wakeUp();
        assertEquals(1, fake.calls.get());
        assertEquals(List.of(), queue.poll(TIMEOUT_MILLIS, 0));
        assertEquals(1, fake.calls.get());

        final AtomicReference<Object> result = new AtomicReference<>("not finished");
        final Thread poller = startPoll(queue, -1, result);
        awaitPollInProgress(queue);
        queue.wakeUp();
        poller.join(TIMEOUT_MILLIS);
        assertFalse(poller.isAlive());
        assertEquals(List.of(), result.get());
        assertEquals(2, fake.calls.get());

        post(queue);
        assertEquals(3, fake.calls.get());
    }

    @Test
    public void closeOfAnArmedQueueSignalsOnce() throws Exception {
        final EventQueue queue = EventQueue.create();
        final FakeSignaller fake = set(queue);

        queue.close();
        assertEquals(1, fake.calls.get());
        assertNull(read(queue, "setting"));
        assertNull(queue.poll(0, 0));
        queue.close();
        post(queue);
        queue.wakeUp();
        assertEquals(1, fake.calls.get());
    }

    @Test
    public void closeSignalsBeforeThePendingTasksRun() throws Exception {
        final EventQueue queue = EventQueue.create();
        final FakeSignaller fake = set(queue);
        final AtomicInteger callsWhenRun = new AtomicInteger(-1);
        post(queue);
        queue.getExecutor().execute(() -> callsWhenRun.set(fake.calls.get()));
        assertEquals(1, fake.calls.get());

        queue.close();

        assertEquals(2, callsWhenRun.get());
        assertEquals(2, fake.calls.get());
        assertNull(queue.poll(0, 0));
    }

    @Test
    public void pollsReturnClosedOnlyAfterTheSignalOfTheCloseReturns() throws Exception {
        final EventQueue queue = EventQueue.create();
        final FakeSignaller fake = set(queue);
        final AtomicReference<Object> waiting = new AtomicReference<>("not finished");
        final Thread waitingPoller = startPoll(queue, -1, waiting);
        awaitPollInProgress(queue);

        fake.block();
        final Thread closer = start(queue::close);
        fake.awaitEntered();
        assertTrue(queue.isClosed());
        final AtomicReference<Object> later = new AtomicReference<>("not finished");
        final Thread laterPoller = startPoll(queue, 0, later);

        waitingPoller.join(BLOCKED_MILLIS);
        laterPoller.join(BLOCKED_MILLIS);
        assertTrue(waitingPoller.isAlive());
        assertTrue(laterPoller.isAlive());
        assertTrue(closer.isAlive());

        fake.unblock();
        closer.join(TIMEOUT_MILLIS);
        waitingPoller.join(TIMEOUT_MILLIS);
        laterPoller.join(TIMEOUT_MILLIS);
        assertFalse(closer.isAlive());
        assertFalse(waitingPoller.isAlive());
        assertFalse(laterPoller.isAlive());
        assertNull(waiting.get());
        assertNull(later.get());

        assertEquals(1, fake.calls.get());
        post(queue);
        queue.wakeUp();
        queue.close();
        assertNull(queue.poll(0, 0));
        assertEquals(1, fake.calls.get());
    }

    @Test
    public void closeDuringAPollThatHoldsEventsSignalsOnceAndTheNextPollReturnsClosed() throws Exception {
        final DXFeedSubscription<Quote> subscription = endpoint.getFeed().createSubscription(Quote.class);
        final EventQueue queue = EventQueue.create();
        queue.attach(subscription, 1);
        final CountDownLatch entered = new CountDownLatch(1);
        final CountDownLatch release = new CountDownLatch(1);
        // Runs after the queue's own listener: the poll holds the events when it blocks.
        subscription.addEventListener(events -> {
            entered.countDown();
            awaitQuietly(release);
        });
        subscription.addSymbols("A");
        final FakeSignaller fake = set(queue);
        publisher.publishEvents(List.of(quote("A", 1)));

        final AtomicReference<Object> result = new AtomicReference<>("not finished");
        final Thread poller = startPoll(queue, TIMEOUT_MILLIS, result);
        assertTrue(entered.await(TIMEOUT_MILLIS, TimeUnit.MILLISECONDS));
        final int before = fake.calls.get();
        assertTrue(before >= 1);

        queue.close();
        assertEquals(before + 1, fake.calls.get());
        release.countDown();
        poller.join(TIMEOUT_MILLIS);
        assertFalse(poller.isAlive());

        assertEquals(1, castEvents(result.get()).size());
        assertNull(queue.poll(0, 0));
        assertEquals(before + 1, fake.calls.get());
    }

    @Test
    public void closeDuringAPollThatRunsAQueueTaskSignalsOnceAndTheNextPollReturnsClosed() throws Exception {
        final EventQueue queue = EventQueue.create();
        final FakeSignaller fake = set(queue);
        final CountDownLatch entered = new CountDownLatch(1);
        final CountDownLatch release = new CountDownLatch(1);
        queue.getExecutor().execute(() -> {
            entered.countDown();
            awaitQuietly(release);
        });
        assertEquals(1, fake.calls.get());

        final AtomicReference<Object> result = new AtomicReference<>("not finished");
        final Thread poller = startPoll(queue, TIMEOUT_MILLIS, result);
        assertTrue(entered.await(TIMEOUT_MILLIS, TimeUnit.MILLISECONDS));

        queue.close();
        assertEquals(2, fake.calls.get());
        release.countDown();
        poller.join(TIMEOUT_MILLIS);
        assertFalse(poller.isAlive());

        assertEquals(List.of(), result.get());
        assertNull(queue.poll(0, 0));
        assertEquals(2, fake.calls.get());
    }

    @Test
    public void closeWaitsForAWriteInProgressAndNoneFollows() throws Exception {
        final EventQueue queue = EventQueue.create();
        final FakeSignaller fake = set(queue);
        fake.block();
        final Thread poster = start(() -> post(queue));
        fake.awaitEntered();

        // The write of the close itself does not block, the write of the poster does.
        final Thread closer = start(queue::close);
        closer.join(BLOCKED_MILLIS);
        assertTrue(closer.isAlive());
        assertEquals(2, fake.calls.get());

        fake.unblock();
        closer.join(TIMEOUT_MILLIS);
        poster.join(TIMEOUT_MILLIS);
        assertFalse(closer.isAlive());
        assertFalse(poster.isAlive());
        post(queue);
        queue.wakeUp();
        assertNull(queue.poll(0, 0));
        assertEquals(2, fake.calls.get());
    }

    @Test
    public void removalWaitsForAWriteOfTheOldSetting() throws Exception {
        final EventQueue queue = EventQueue.create();
        final FakeSignaller old = set(queue);
        old.block();
        final Thread poster = start(() -> post(queue));
        old.awaitEntered();

        final Thread remover = start(() -> queue.setReadySignal(null));
        remover.join(BLOCKED_MILLIS);
        assertTrue(remover.isAlive());

        old.unblock();
        remover.join(TIMEOUT_MILLIS);
        poster.join(TIMEOUT_MILLIS);
        assertFalse(remover.isAlive());
        assertFalse(poster.isAlive());
        assertNull(read(queue, "setting"));

        queue.poll(0, 0);
        post(queue);
        queue.wakeUp();
        queue.close();
        assertEquals(1, old.calls.get());
    }

    @Test
    public void replacementWaitsForAWriteOfTheOldSettingAndSignalsForPendingWork() throws Exception {
        final EventQueue queue = EventQueue.create();
        final FakeSignaller old = set(queue);
        old.block();
        final Thread poster = start(() -> post(queue));
        old.awaitEntered();

        final FakeSignaller replacement = new FakeSignaller(queue);
        final Thread replacer = start(() -> queue.setReadySignal(replacement));
        replacer.join(BLOCKED_MILLIS);
        assertTrue(replacer.isAlive());
        assertEquals(0, replacement.calls.get());

        old.unblock();
        replacer.join(TIMEOUT_MILLIS);
        poster.join(TIMEOUT_MILLIS);
        assertFalse(replacer.isAlive());
        assertFalse(poster.isAlive());
        // The task posted before the replacement is pending: the new setting signals at once.
        assertEquals(1, replacement.calls.get());

        queue.poll(0, 0);
        post(queue);
        assertEquals(2, replacement.calls.get());
        assertEquals(1, old.calls.get());
        queue.close();
        assertEquals(3, replacement.calls.get());
        assertEquals(1, old.calls.get());
    }

    @Test
    public void removalOrReplacementRacingACloseWaitsForItsWrite() throws Exception {
        final EventQueue queue = EventQueue.create();
        final FakeSignaller fake = set(queue);
        fake.block();
        final Thread closer = start(queue::close);
        fake.awaitEntered();

        final Thread remover = start(() -> queue.setReadySignal(null));
        final FakeSignaller replacement = new FakeSignaller(queue);
        final Thread replacer = start(() -> queue.setReadySignal(replacement));
        awaitWaiting(remover);
        awaitWaiting(replacer);
        assertEquals(0, replacement.calls.get());

        fake.unblock();
        closer.join(TIMEOUT_MILLIS);
        remover.join(TIMEOUT_MILLIS);
        replacer.join(TIMEOUT_MILLIS);
        assertFalse(closer.isAlive());
        assertFalse(remover.isAlive());
        assertFalse(replacer.isAlive());
        assertEquals(1, fake.calls.get());
        // A signal set on a closed queue is called once and not kept.
        assertEquals(1, replacement.calls.get());
        assertNull(read(queue, "setting"));
    }

    @Test
    public void closeAfterAReplacementWaitsForAWriteOfTheOldSetting() throws Exception {
        final EventQueue queue = EventQueue.create();
        final FakeSignaller old = set(queue);
        old.block();
        final Thread poster = start(() -> post(queue));
        old.awaitEntered();
        // The replacement takes the setting and waits for the old one, holding its own write for the pending task.
        final FakeSignaller replacement = new FakeSignaller(queue);
        final Thread replacer = start(() -> queue.setReadySignal(replacement));
        awaitWaiting(replacer);

        // The close takes the replacement: it signals it and waits for the replacer's write, which waits for the old one.
        final Thread closer = start(queue::close);
        awaitWaiting(closer);
        assertEquals(1, replacement.calls.get());

        old.unblock();
        for (final Thread thread : List.of(poster, replacer, closer)) {
            thread.join(TIMEOUT_MILLIS);
            assertFalse(thread.isAlive());
        }
        assertEquals(1, old.calls.get());
        assertEquals(2, replacement.calls.get());
        assertNull(read(queue, "setting"));
    }

    @Test
    public void secondCloseWaitsForTheWriteOfTheFirst() throws Exception {
        final EventQueue queue = EventQueue.create();
        final FakeSignaller fake = set(queue);
        fake.block();
        final Thread closer = start(queue::close);
        fake.awaitEntered();

        final Thread secondCloser = start(queue::close);
        awaitWaiting(secondCloser);

        fake.unblock();
        closer.join(TIMEOUT_MILLIS);
        secondCloser.join(TIMEOUT_MILLIS);
        assertFalse(closer.isAlive());
        assertFalse(secondCloser.isAlive());
        assertEquals(1, fake.calls.get());
    }

    @Test
    public void pollAndCloseWaitForAWriteOfASettingRemovedBefore() throws Exception {
        final EventQueue queue = EventQueue.create();
        final FakeSignaller old = set(queue);
        old.block();
        final Thread poster = start(() -> post(queue));
        old.awaitEntered();
        final Thread remover = start(() -> queue.setReadySignal(null));
        awaitWaiting(remover);

        // The close finds no setting, yet the removed one is still written to.
        final Thread closer = start(queue::close);
        awaitWaiting(closer);
        final AtomicReference<Object> result = new AtomicReference<>("not returned");
        final Thread poller = startPoll(queue, TIMEOUT_MILLIS, result);
        awaitWaiting(poller);
        assertEquals("not returned", result.get());

        old.unblock();
        for (final Thread thread : List.of(poster, remover, closer, poller)) {
            thread.join(TIMEOUT_MILLIS);
            assertFalse(thread.isAlive());
        }
        assertNull(result.get());
        assertEquals(1, old.calls.get());
    }

    @Test
    public void pollWaitsForTheWriteOfASignalSetOnAClosedQueue() throws Exception {
        final EventQueue queue = EventQueue.create();
        queue.close();
        final FakeSignaller late = new FakeSignaller(queue);
        late.block();
        final Thread setter = start(() -> queue.setReadySignal(late));
        late.awaitEntered();

        final AtomicReference<Object> result = new AtomicReference<>("not returned");
        final Thread poller = startPoll(queue, TIMEOUT_MILLIS, result);
        awaitWaiting(poller);
        assertEquals("not returned", result.get());

        late.unblock();
        setter.join(TIMEOUT_MILLIS);
        poller.join(TIMEOUT_MILLIS);
        assertFalse(setter.isAlive());
        assertFalse(poller.isAlive());
        assertNull(result.get());
        assertEquals(1, late.calls.get());
    }

    @Test
    public void failedWriteOfARemovedSettingDoesNotArmTheQueue() throws Exception {
        final EventQueue queue = EventQueue.create();
        final FakeSignaller old = set(queue);
        old.failure = "write failed, errno 9";
        old.block();
        final AtomicReference<Throwable> posterFailure = new AtomicReference<>();
        final Thread poster = start(() -> postCatching(queue, posterFailure));
        old.awaitEntered();
        final Thread remover = start(() -> queue.setReadySignal(null));
        awaitWaiting(remover);

        old.unblock();
        poster.join(TIMEOUT_MILLIS);
        remover.join(TIMEOUT_MILLIS);
        assertFalse(poster.isAlive());
        assertFalse(remover.isAlive());
        assertNull(posterFailure.get());
        assertEquals(false, read(queue, "armed"));

        queue.poll(0, 0);
        post(queue);
        assertEquals(1, old.calls.get());
        assertEquals(1, logSink.records().size());
    }

    @Test
    public void failedWriteOfAReplacedSettingDoesNotArmTheQueue() throws Exception {
        final EventQueue queue = EventQueue.create();
        final FakeSignaller old = set(queue);
        old.failure = "write failed, errno 9";
        old.block();
        final AtomicReference<Throwable> posterFailure = new AtomicReference<>();
        final Thread poster = start(() -> postCatching(queue, posterFailure));
        old.awaitEntered();
        final FakeSignaller replacement = new FakeSignaller(queue);
        final Thread replacer = start(() -> queue.setReadySignal(replacement));
        awaitWaiting(replacer);

        old.unblock();
        poster.join(TIMEOUT_MILLIS);
        replacer.join(TIMEOUT_MILLIS);
        assertFalse(poster.isAlive());
        assertFalse(replacer.isAlive());
        assertNull(posterFailure.get());
        // The replacement was signalled for the pending task: the queue is disarmed until the next poll.
        assertEquals(1, replacement.calls.get());
        assertEquals(false, read(queue, "armed"));

        queue.poll(0, 0);
        post(queue);
        assertEquals(2, replacement.calls.get());
        assertEquals(1, old.calls.get());
        assertEquals(1, logSink.records().size());
    }

    @Test
    public void pollEndingWithAnExceptionSignalsTheWorkLeftBehind() throws Exception {
        final EventQueue queue = EventQueue.create();
        final FakeSignaller fake = set(queue);
        queue.getExecutor().execute(() -> {
            throw new IllegalStateException("task");
        });
        post(queue);
        assertEquals(1, fake.calls.get());

        assertThrows(IllegalStateException.class, () -> queue.poll(0, 0));
        // The second task is left behind.
        assertEquals(2, fake.calls.get());

        queue.poll(0, 0);
        assertEquals(true, read(queue, "armed"));
        post(queue);
        assertEquals(3, fake.calls.get());
    }

    @Test
    public void failedWriteArmsTheQueueAgainAndIsLoggedOncePerSetting() throws Exception {
        final EventQueue queue = EventQueue.create();
        final FakeSignaller failing = set(queue);
        failing.failure = "write failed, errno 9";

        post(queue);
        assertEquals(1, failing.calls.get());
        post(queue);
        assertEquals(2, failing.calls.get());
        post(queue);
        assertEquals(3, failing.calls.get());
        assertEquals(1, logSink.records().size());
        assertTrue(logSink.records().get(0).getMessage().contains("write failed, errno 9"));

        queue.poll(0, 0);
        final FakeSignaller next = set(queue);
        next.failure = "write failed, errno 32";
        post(queue);
        post(queue);
        assertEquals(2, next.calls.get());
        assertEquals(3, failing.calls.get());
        assertEquals(2, logSink.records().size());
        assertTrue(logSink.records().get(1).getMessage().contains("write failed, errno 32"));

        // A signal that works again is not logged.
        next.failure = null;
        queue.poll(0, 0);
        post(queue);
        assertEquals(3, next.calls.get());
        assertEquals(2, logSink.records().size());
    }

    @Test
    public void blockingLogSinkDoesNotHoldUpRemovalOrClose() throws Exception {
        logSink.block();
        final EventQueue removed = EventQueue.create();
        final FakeSignaller removedFake = set(removed);
        removedFake.failure = "write failed, errno 9";
        final Thread firstPoster = start(() -> post(removed));
        logSink.awaitEntered();

        final Thread remover = start(() -> removed.setReadySignal(null));
        remover.join(TIMEOUT_MILLIS);
        assertFalse("the removal waits for the log sink", remover.isAlive());

        final EventQueue closed = EventQueue.create();
        final FakeSignaller closedFake = set(closed);
        closedFake.failure = "write failed, errno 9";
        final Thread secondPoster = start(() -> post(closed));
        logSink.awaitEntered();
        final Thread closer = start(closed::close);
        closer.join(TIMEOUT_MILLIS);
        assertFalse("the close waits for the log sink", closer.isAlive());
        assertTrue(firstPoster.isAlive());
        assertTrue(secondPoster.isAlive());

        logSink.release();
        firstPoster.join(TIMEOUT_MILLIS);
        secondPoster.join(TIMEOUT_MILLIS);
        assertFalse(firstPoster.isAlive());
        assertFalse(secondPoster.isAlive());
        assertEquals(1, removedFake.calls.get());
        // The close's own write fails too, and the failure is logged once for the setting.
        assertEquals(2, closedFake.calls.get());
    }

    @Test
    public void settingFromAListenerInsideAPollReturnsAndTakesEffect() throws Exception {
        final DXFeedSubscription<Quote> subscription = endpoint.getFeed().createSubscription(Quote.class);
        final EventQueue queue = EventQueue.create();
        queue.attach(subscription, 1);
        final FakeSignaller first = set(queue);
        final FakeSignaller second = new FakeSignaller(queue);
        final AtomicReference<Throwable> failure = new AtomicReference<>();
        final AtomicBoolean replaced = new AtomicBoolean();
        subscription.addEventListener(events -> {
            try {
                queue.setReadySignal(second);
                replaced.set(true);
            } catch (final Throwable e) {
                failure.set(e);
            }
        });
        subscription.addSymbols("A");

        publisher.publishEvents(List.of(quote("A", 1)));
        assertEquals(1, first.calls.get());
        assertEquals(1, queue.poll(TIMEOUT_MILLIS, 0).size());
        assertNull(failure.get());
        assertTrue(replaced.get());

        post(queue);
        assertEquals(1, second.calls.get());
        assertEquals(1, first.calls.get());

        // The same from a task of the queue-level executor: the removal takes effect.
        queue.getExecutor().execute(() -> queue.setReadySignal(null));
        assertEquals(1, second.calls.get());
        queue.poll(TIMEOUT_MILLIS, 0);
        assertNull(read(queue, "setting"));
        post(queue);
        assertEquals(1, second.calls.get());
    }

    @Test
    public void concurrentProducersNeverLoseASignal() throws Exception {
        for (int i = 0; i < CONCURRENT_ITERATIONS; i++) {
            runConcurrentIteration();
        }
    }

    // Four producers post tasks and hand events to the queue's listener from their own threads (held events), the
    // consumer is an event loop: it waits only on the fake's semaphore, resets it and polls with the timeout 0.
    private void runConcurrentIteration() throws Exception {
        final DXFeedSubscription<Quote> subscription = endpoint.getFeed().createSubscription(Quote.class);
        final EventQueue queue = EventQueue.create();
        queue.attach(subscription, 7);
        final DXFeedEventListener<EventType<?>> listener = attachmentListener(queue);
        final FakeSignaller fake = set(queue);
        final int tasksExpected = PRODUCERS * OPERATIONS_PER_PRODUCER / 2;
        final int eventsExpected = PRODUCERS * OPERATIONS_PER_PRODUCER / 2;
        final AtomicInteger tasksRun = new AtomicInteger();
        final CountDownLatch go = new CountDownLatch(1);
        final List<Thread> producers = new ArrayList<>();
        for (int p = 0; p < PRODUCERS; p++) {
            producers.add(start(() -> {
                awaitQuietly(go);
                for (int k = 0; k < OPERATIONS_PER_PRODUCER; k++) {
                    if (k % 2 == 0) {
                        queue.getExecutor().execute(tasksRun::incrementAndGet);
                    } else {
                        listener.eventsReceived(List.of(quote("A", k)));
                    }
                }
            }));
        }

        go.countDown();
        int events = 0;
        while (tasksRun.get() < tasksExpected || events < eventsExpected) {
            assertTrue("a signal is lost: tasks " + tasksRun.get() + " of " + tasksExpected + ", events " + events
                    + " of " + eventsExpected, fake.signals.tryAcquire(TIMEOUT_MILLIS, TimeUnit.MILLISECONDS));
            fake.signals.drainPermits();
            events += queue.poll(0, 0).size();
            assertWorkImpliesDisarmedAndSignalled(queue, fake);
        }
        for (final Thread producer : producers) {
            producer.join(TIMEOUT_MILLIS);
            assertFalse(producer.isAlive());
        }
        assertEquals(eventsExpected, events);
        assertEquals(tasksExpected, tasksRun.get());
        queue.close();
        subscription.close();
    }

    // At the end of a poll: work in the queue means the queue is disarmed and a signal has been given since the
    // consumer reset the fake (a token taken but not yet written counts: the write follows the lock).
    private static void assertWorkImpliesDisarmedAndSignalled(final EventQueue queue, final FakeSignaller fake)
            throws ReflectiveOperationException {
        synchronized (read(queue, "lock")) {
            final boolean work = !((Collection<?>) read(queue, "tasks")).isEmpty()
                    || !((EventQueue.Batch) read(queue, "held")).isEmpty();
            if (work) {
                assertFalse("armed with work in the queue", (Boolean) read(queue, "armed"));
                final Object setting = read(queue, "setting");
                final Field inFlight = setting.getClass().getDeclaredField("inFlight");
                inFlight.setAccessible(true);
                assertTrue("work without a signal", fake.signals.availablePermits() + inFlight.getInt(setting) > 0);
            }
        }
    }

    @Test
    public void shutdownSignalsOnceAndThePollsReturnClosed() throws Exception {
        final EventQueue queue = EventQueue.create();
        final FakeSignaller fake = set(queue);
        try {
            NativeListener.deactivateAll();
            EventQueue.closeAll();

            assertEquals(1, fake.calls.get());
            assertNull(queue.poll(TIMEOUT_MILLIS, 0));
            assertEquals(1, fake.calls.get());

            // A queue created after the shutdown is closed: its setting writes once and is not kept.
            final EventQueue later = EventQueue.create();
            final FakeSignaller laterFake = set(later);
            assertEquals(1, laterFake.calls.get());
            assertNull(read(later, "setting"));
            assertNull(later.poll(0, 0));
        } finally {
            undoCloseAll();
            undoDeactivateAll();
        }
    }

    private FakeSignaller set(final EventQueue queue) throws ReflectiveOperationException {
        final FakeSignaller fake = new FakeSignaller(queue);
        queue.setReadySignal(fake);
        return fake;
    }

    private static void post(final EventQueue queue) {
        queue.getExecutor().execute(() -> { });
    }

    private static void postCatching(final EventQueue queue, final AtomicReference<Throwable> failure) {
        try {
            post(queue);
        } catch (final RuntimeException | Error e) {
            failure.set(e);
        }
    }

    private static Quote quote(final String symbol, final double bidPrice) {
        final Quote quote = new Quote(symbol);
        quote.setBidPrice(bidPrice);
        return quote;
    }

    @SuppressWarnings("unchecked")
    private static List<EventType<?>> castEvents(final Object result) {
        return (List<EventType<?>>) result;
    }

    // The queue's own listener of the attached subscription: a delivery from the thread that calls it.
    @SuppressWarnings("unchecked")
    private static DXFeedEventListener<EventType<?>> attachmentListener(final EventQueue queue)
            throws ReflectiveOperationException {
        final Object attachment;
        synchronized (read(queue, "lock")) {
            attachment = ((List<?>) read(queue, "attachments")).get(0);
        }
        final Field listener = attachment.getClass().getDeclaredField("listener");
        listener.setAccessible(true);
        return (DXFeedEventListener<EventType<?>>) listener.get(attachment);
    }

    // The value of a field of the queue, read under the queue's lock (the field "lock" itself is the lock).
    private static Object read(final EventQueue queue, final String name) throws ReflectiveOperationException {
        final Field lock = EventQueue.class.getDeclaredField("lock");
        lock.setAccessible(true);
        if (name.equals("lock")) {
            return lock.get(queue);
        }
        final Field field = EventQueue.class.getDeclaredField(name);
        field.setAccessible(true);
        synchronized (lock.get(queue)) {
            return field.get(queue);
        }
    }

    private static Collection<?> pendingTasks(final EventQueue queue) throws ReflectiveOperationException {
        return new ArrayList<>((Collection<?>) read(queue, "tasks"));
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
        awaitCondition(() -> read(queue, "pollThread") != null);
    }

    // Waits until the thread waits on a monitor (the queue's lock); fails if it ends instead.
    private static void awaitWaiting(final Thread thread) throws Exception {
        awaitCondition(() -> {
            assertTrue("the thread ended instead of waiting", thread.isAlive());
            return thread.getState() == Thread.State.WAITING;
        });
    }

    private static Thread start(final Runnable task) {
        final Thread thread = new Thread(task);
        thread.setDaemon(true);
        thread.start();
        return thread;
    }

    private static void awaitQuietly(final CountDownLatch latch) {
        try {
            latch.await(TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static void awaitCondition(final Condition condition) throws Exception {
        final long deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(TIMEOUT_MILLIS);
        while (!condition.test()) {
            assertTrue("condition not met", System.nanoTime() < deadline);
            Thread.sleep(5);
        }
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

    // Also empties the registry: the deactivated listeners stay there until they are collected.
    private static void undoDeactivateAll() throws ReflectiveOperationException {
        final Field lock = NativeListener.class.getDeclaredField("REGISTRY_LOCK");
        final Field listeners = NativeListener.class.getDeclaredField("LISTENERS");
        final Field allDeactivated = NativeListener.class.getDeclaredField("allDeactivated");
        lock.setAccessible(true);
        listeners.setAccessible(true);
        allDeactivated.setAccessible(true);
        synchronized (lock.get(null)) {
            ((Set<?>) listeners.get(null)).clear();
            allDeactivated.setBoolean(null, false);
        }
    }

    private interface Condition {
        boolean test() throws Exception;
    }

    // A signaller that counts its calls and can block inside signal() or fail.
    private final class FakeSignaller implements EventQueue.Signaller {
        final AtomicInteger calls = new AtomicInteger();
        // A permit per call, given before the call can block: the fake of an event loop's OS object.
        final Semaphore signals = new Semaphore(0);
        final AtomicBoolean wroteUnderLock = new AtomicBoolean();
        private final Semaphore entered = new Semaphore(0);
        private final Object queueLock;
        volatile Thread lastThread;
        volatile String failure;
        private final AtomicReference<CountDownLatch> gate = new AtomicReference<>();
        private volatile CountDownLatch blocking;

        FakeSignaller(final EventQueue queue) throws ReflectiveOperationException {
            queueLock = read(queue, "lock");
            fakes.add(this);
        }

        @Override
        public String signal() {
            calls.incrementAndGet();
            lastThread = Thread.currentThread();
            if (Thread.holdsLock(queueLock)) {
                wroteUnderLock.set(true);
            }
            signals.release();
            entered.release();
            final CountDownLatch current = gate.getAndSet(null);
            if (current != null) {
                awaitQuietly(current);
            }
            return failure;
        }

        // The next call blocks until unblock(), the calls after it do not.
        void block() {
            blocking = new CountDownLatch(1);
            gate.set(blocking);
        }

        void unblock() {
            blocking.countDown();
        }

        void awaitEntered() throws InterruptedException {
            assertTrue("signal() is not called", entered.tryAcquire(TIMEOUT_MILLIS, TimeUnit.MILLISECONDS));
        }
    }

    // Collects the log records of the queue; can block every record until released.
    private static final class LogSink extends Handler {
        private final List<LogRecord> records = new ArrayList<>();
        private final Semaphore entered = new Semaphore(0);
        private volatile CountDownLatch gate;

        @Override
        public void publish(final LogRecord record) {
            synchronized (records) {
                records.add(record);
            }
            entered.release();
            final CountDownLatch current = gate;
            if (current != null) {
                awaitQuietly(current);
            }
        }

        @Override
        public void flush() {
        }

        @Override
        public void close() {
        }

        List<LogRecord> records() {
            synchronized (records) {
                return new ArrayList<>(records);
            }
        }

        void block() {
            gate = new CountDownLatch(1);
        }

        void release() {
            final CountDownLatch current = gate;
            if (current != null) {
                current.countDown();
            }
        }

        void awaitEntered() throws InterruptedException {
            assertTrue("nothing is logged", entered.tryAcquire(TIMEOUT_MILLIS, TimeUnit.MILLISECONDS));
        }
    }
}
