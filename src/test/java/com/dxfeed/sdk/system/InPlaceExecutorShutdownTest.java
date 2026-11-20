// Copyright (c) 2026 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

package com.dxfeed.sdk.system;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;

import com.dxfeed.api.DXEndpoint;
import com.dxfeed.api.DXFeedSubscription;
import com.dxfeed.event.market.Quote;
import com.dxfeed.sdk.javac.InPlaceExecutor;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.Test;

// MDAPI-603: QD waits on close until the endpoint's executor runs the tasks it schedules, which an in-place executor
// does only when asked.
public class InPlaceExecutorShutdownTest {

    private static final long TIMEOUT_MILLIS = 10_000;

    private interface Close {
        void run(DXEndpoint endpoint) throws InterruptedException;
    }

    private static DXEndpoint newEndpoint(final DXEndpoint.Role role, final InPlaceExecutor executor) {
        return IsolateResources.register(DXEndpoint.newBuilder().withRole(role).withExecutor(executor).build());
    }

    private static void closeWithinTimeout(final DXEndpoint endpoint, final Close close) throws Exception {
        final AtomicReference<Throwable> failure = new AtomicReference<>();
        final Thread closer = new Thread(() -> {
            try {
                close.run(endpoint);
            } catch (final Throwable e) {
                failure.set(e);
            }
        }, "test-close");
        closer.setDaemon(true);
        closer.start();
        closer.join(TIMEOUT_MILLIS);
        assertFalse("The close did not return in " + TIMEOUT_MILLIS + " ms", closer.isAlive());
        if (failure.get() != null) {
            throw new AssertionError("The close failed", failure.get());
        }
        assertEquals(DXEndpoint.State.CLOSED, endpoint.getState());
    }

    @Test
    public void closeAndAwaitTerminationReturnsForFeedEndpoint() throws Exception {
        closeWithinTimeout(newEndpoint(DXEndpoint.Role.FEED, new InPlaceExecutor()),
            IsolateResources::closeAndAwaitTermination);
    }

    @Test
    public void closeAndAwaitTerminationReturnsForLocalHubAndNotifiesOnCallingThread() throws Exception {
        final DXEndpoint endpoint = newEndpoint(DXEndpoint.Role.LOCAL_HUB, new InPlaceExecutor());
        final List<Thread> notifiedThreads = new CopyOnWriteArrayList<>();
        endpoint.addStateChangeListener(event -> {
            if (event.getNewValue() == DXEndpoint.State.CLOSED) {
                notifiedThreads.add(Thread.currentThread());
            }
        });
        final AtomicReference<Thread> callingThread = new AtomicReference<>();
        closeWithinTimeout(endpoint, closed -> {
            callingThread.set(Thread.currentThread());
            IsolateResources.closeAndAwaitTermination(closed);
        });
        assertEquals(1, notifiedThreads.size());
        assertSame(callingThread.get(), notifiedThreads.get(0));
    }

    @Test
    public void closeAndAwaitTerminationReturnsWithUndeliveredEvents() throws Exception {
        final DXEndpoint endpoint = newEndpoint(DXEndpoint.Role.LOCAL_HUB, new InPlaceExecutor());
        final List<Quote> received = new CopyOnWriteArrayList<>();
        final DXFeedSubscription<Quote> subscription = endpoint.getFeed().createSubscription(Quote.class);
        subscription.addEventListener(received::addAll);
        subscription.addSymbols("TEST");
        endpoint.getPublisher().publishEvents(Collections.singletonList(new Quote("TEST")));
        closeWithinTimeout(endpoint, IsolateResources::closeAndAwaitTermination);
    }

    @Test
    public void closeAllAndAwaitTerminationReturns() throws Exception {
        final DXEndpoint feed = newEndpoint(DXEndpoint.Role.FEED, new InPlaceExecutor());
        final DXEndpoint hub = newEndpoint(DXEndpoint.Role.LOCAL_HUB, new InPlaceExecutor());
        closeWithinTimeout(hub, ignored -> IsolateResources.closeAllAndAwaitTermination());
        assertEquals(DXEndpoint.State.CLOSED, feed.getState());
    }

    // Another thread that runs the queued tasks in processAllPendingTasks may take the wake-up task.
    @Test
    public void processTasksUntilDoneReturnsWithoutTheWakeUpTask() throws Exception {
        final InPlaceExecutor executor = new InPlaceExecutor();
        final CompletableFuture<Void> done = new CompletableFuture<>();
        final Thread waiter = new Thread(() -> {
            try {
                executor.processTasksUntilDone(done);
            } catch (final InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }, "test-wait");
        waiter.setDaemon(true);
        waiter.start();
        while (waiter.getState() != Thread.State.WAITING && waiter.getState() != Thread.State.TIMED_WAITING) {
            Thread.sleep(1);
        }
        done.complete(null);
        waiter.join(TIMEOUT_MILLIS);
        assertFalse("processTasksUntilDone did not return in " + TIMEOUT_MILLIS + " ms", waiter.isAlive());
    }

    @Test
    public void closeAndAwaitTerminationKeepsWorkingForDefaultExecutor() throws Exception {
        final DXEndpoint endpoint = IsolateResources.register(DXEndpoint.create(DXEndpoint.Role.LOCAL_HUB));
        closeWithinTimeout(endpoint, IsolateResources::closeAndAwaitTermination);
    }
}
