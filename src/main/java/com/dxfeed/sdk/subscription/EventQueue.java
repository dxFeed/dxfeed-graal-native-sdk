// Copyright (c) 2026 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

package com.dxfeed.sdk.subscription;

import com.dxfeed.api.DXFeed;
import com.dxfeed.api.DXFeedEventListener;
import com.dxfeed.api.DXFeedSubscription;
import com.dxfeed.api.osub.ObservableSubscriptionChangeListener;
import com.dxfeed.event.EventType;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.Executor;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * A queue of the events of one subscription that the host drains on its own thread (dxfg_v4_event_queue.h).
 *
 * <p>The queue is the subscription's executor: QD posts its "retrieve the data" tasks to it, and {@link #drain}
 * runs them on the calling thread, so QD retrieves the events from its buffers only when the host drains.
 * QD conflates the lasting events and counts the events its buffers drop; the queue adds no buffer of its own.
 * An internal listener collects the events the tasks deliver; the other listeners of the subscription are called
 * by the same tasks, so they run on the draining thread too.
 *
 * <p>A closed queue runs every task posted to it at once, on the posting thread, and its listener discards the
 * events and counts them as dropped: QD's {@code closeAndAwaitTermination} and
 * {@code detachSubscriptionAndClear} wait until no task of the subscription is pending.
 *
 * <p>{@link #closeAll()} closes all the queues of the isolate (the static fields are per isolate), and the queues
 * created after it are closed from the start.
 */
public final class EventQueue {

    private static final long MAX_TIMEOUT_MILLIS = Long.MAX_VALUE / 1_000_000;

    private static final Object REGISTRY_LOCK = new Object();
    // Guarded by REGISTRY_LOCK. Weak: an unreachable queue has no task to run.
    private static final Set<EventQueue> QUEUES = Collections.newSetFromMap(new WeakHashMap<>());
    private static boolean allClosed;

    private final DXFeedSubscription<?> subscription;
    private final QueueExecutor executor = new QueueExecutor();

    private final Object lock = new Object();
    // Guarded by lock.
    private final ArrayDeque<Runnable> tasks = new ArrayDeque<>();
    private final List<EventType<?>> held = new ArrayList<>(); // delivered outside a drain, go to the next one
    private List<EventType<?>> batch; // the batch of the drain in progress
    private Thread drainThread; // the thread of the drain in progress
    private boolean closed;
    private boolean wakeUp;
    private int detaching;
    private int handles;
    private long subscriptionDropped; // QD's total as of the last time the tasks ran
    private long queueDropped; // the events the queue itself dropped

    private EventQueue(final DXFeedSubscription<?> subscription) {
        this.subscription = subscription;
    }

    /**
     * Creates a queue on the subscription.
     *
     * @throws IllegalStateException if the subscription has symbols, a queue or an executor.
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static EventQueue create(final DXFeedSubscription<?> subscription) {
        synchronized (REGISTRY_LOCK) {
            if (subscription.getExecutor() instanceof QueueExecutor) {
                throw new IllegalStateException("The subscription already has an event queue");
            }
            if (subscription.getExecutor() != null) {
                throw new IllegalStateException("The subscription has an executor, an event queue needs its own");
            }
            if (!subscription.getSymbols().isEmpty()) {
                throw new IllegalStateException("An event queue must be created before the symbols are added");
            }

            final EventQueue queue = new EventQueue(subscription);
            final DXFeedSubscription raw = subscription;
            raw.addEventListener(queue.new QueueListener());
            subscription.setExecutor(queue.executor);
            subscription.addChangeListener(queue.new CloseListener());
            if (allClosed || subscription.isClosed()) {
                queue.closed = true;
            } else {
                QUEUES.add(queue);
            }
            return queue;
        }
    }

    /**
     * @return the queue of the subscription or {@code null} if it has none.
     */
    public static EventQueue of(final DXFeedSubscription<?> subscription) {
        final Executor current = subscription.getExecutor();
        return current instanceof QueueExecutor ? ((QueueExecutor) current).queue() : null;
    }

    /**
     * Sets the executor of a subscription (or of its subscription controller) unless the subscription has a queue.
     *
     * @throws IllegalStateException if the subscription has a queue or the executor is the one of a queue.
     */
    public static void setExecutor(final Supplier<Executor> current, final Consumer<Executor> setter,
            final Executor executor) {
        synchronized (REGISTRY_LOCK) {
            if (current.get() instanceof QueueExecutor) {
                throw new IllegalStateException("The subscription has an event queue, its executor cannot be changed");
            }
            if (executor instanceof QueueExecutor) {
                throw new IllegalStateException("The executor of an event queue cannot be set on a subscription");
            }
            setter.accept(executor);
        }
    }

    /**
     * Calls {@link DXFeed#detachSubscriptionAndClear} without waiting for a drain: the tasks pending in the queue
     * and the ones posted while QD detaches run at once, their events go to the next drain.
     */
    public static void detachSubscriptionAndClear(final DXFeed feed, final DXFeedSubscription<?> subscription) {
        final EventQueue queue = of(subscription);
        if (queue == null) {
            feed.detachSubscriptionAndClear(subscription);
            return;
        }

        final List<Runnable> pending;
        synchronized (queue.lock) {
            queue.detaching++;
            pending = queue.takeTasks();
        }
        try {
            runAll(pending);
            feed.detachSubscriptionAndClear(subscription);
        } finally {
            synchronized (queue.lock) {
                queue.detaching--;
            }
        }
    }

    /**
     * Closes all the queues of the isolate, the queues created afterwards are closed from the start.
     * Waits for the drains in progress on the other threads.
     */
    public static void closeAll() {
        final List<EventQueue> queues;
        synchronized (REGISTRY_LOCK) {
            allClosed = true;
            queues = new ArrayList<>(QUEUES);
        }
        for (final EventQueue queue : queues) {
            queue.close();
        }
        for (final EventQueue queue : queues) {
            queue.awaitDrain();
        }
    }

    /**
     * Waits for events and returns them.
     *
     * <p>The drains of one queue are serialized: a drain waits for the one in progress on another thread.
     *
     * @param requestedTimeoutMillis 0 returns at once, a negative value or one over
     *                               {@code Long.MAX_VALUE / 1_000_000} waits until events arrive, a wake-up or a close.
     * @param limit                  the number of events after which no more tasks are run (a batch may exceed it),
     *                               0 for no limit.
     * @return the events, an empty list if there were none before the timeout or a wake-up,
     *         {@code null} if the queue is closed. Once it has events, a drain returns them also if the queue is
     *         closed or detached meanwhile.
     * @throws IllegalStateException if called from a listener of the subscription during a drain of this queue.
     */
    public List<EventType<?>> drain(final long requestedTimeoutMillis, final int limit) throws InterruptedException {
        // A timeout that does not fit in nanoseconds (over 292 years) is an infinite one.
        final long timeoutMillis = requestedTimeoutMillis > MAX_TIMEOUT_MILLIS ? -1 : requestedTimeoutMillis;
        final long deadline = timeoutMillis > 0 ? System.nanoTime() + timeoutMillis * 1_000_000 : 0;
        final Thread current = Thread.currentThread();
        synchronized (lock) {
            if (drainThread == current) {
                throw new IllegalStateException("The event queue is drained from a listener of its own drain");
            }
            while (true) {
                if (closed) {
                    return null;
                }
                if (drainThread == null) {
                    break;
                }
                if (!await(timeoutMillis, deadline)) {
                    return Collections.emptyList();
                }
            }
            drainThread = current;
            batch = new ArrayList<>();
        }

        try {
            // Once there are events, only the tasks pending at that moment are run: QD posts a new task while its
            // buffer has data, so a drain without a limit would not return under a steady flow.
            int remainingTasks = -1;
            while (true) {
                final Runnable task;
                synchronized (lock) {
                    if (!held.isEmpty()) {
                        batch.addAll(held);
                        held.clear();
                    }
                    if (!batch.isEmpty()) {
                        if (remainingTasks < 0) {
                            remainingTasks = tasks.size();
                        }
                        if (remainingTasks == 0 || limit > 0 && batch.size() >= limit) {
                            wakeUp = false;
                            return batch;
                        }
                        remainingTasks--;
                    }
                    task = tasks.poll();
                    if (task == null) {
                        if (!batch.isEmpty()) {
                            // A close or a detach on another thread took the tasks counted in remainingTasks.
                            wakeUp = false;
                            return batch;
                        }
                        if (closed) {
                            return null;
                        }
                        if (wakeUp || !await(timeoutMillis, deadline)) {
                            wakeUp = false;
                            return Collections.emptyList();
                        }
                        continue;
                    }
                }
                task.run();
                updateSubscriptionDropped();
            }
        } catch (final RuntimeException | Error e) {
            synchronized (lock) {
                queueDropped += batch.size(); // retrieved, but not returned
            }
            throw e;
        } finally {
            synchronized (lock) {
                drainThread = null;
                batch = null;
                lock.notifyAll();
            }
        }
    }

    /**
     * Counts a batch returned by {@link #drain} as dropped: the caller failed to deliver it (e.g. to convert it to the
     * C structures).
     */
    public void dropBatch(final List<EventType<?>> events) {
        synchronized (lock) {
            queueDropped += events.size();
        }
    }

    /**
     * Makes the drain in progress, or the next one if none is waiting, return no events.
     */
    public void wakeUp() {
        synchronized (lock) {
            wakeUp = true;
            lock.notifyAll();
        }
    }

    /**
     * Closes the queue: wakes the drains, runs the pending tasks on this thread discarding their events.
     * The queue stays the executor of the subscription.
     */
    public void close() {
        final List<Runnable> pending;
        synchronized (lock) {
            closed = true;
            lock.notifyAll();
            queueDropped += held.size();
            held.clear();
            pending = takeTasks();
        }
        // Stays in QUEUES (weak): removing it here would take REGISTRY_LOCK under the subscription's lock
        // (subscriptionClosed), the reverse of the order in create().
        try {
            runAll(pending);
        } finally {
            updateSubscriptionDropped();
        }
    }

    public boolean isClosed() {
        synchronized (lock) {
            return closed;
        }
    }

    /**
     * @return the subscription's total of dropped events as of the last time the tasks ran, plus the events the
     *         queue dropped itself (a failed conversion, the events of a closed queue).
     */
    public long getDroppedEvents() {
        synchronized (lock) {
            return subscriptionDropped + queueDropped;
        }
    }

    // Called when a handle of this queue is created (see JavaObjectHandlerMapper).
    public void handleCreated() {
        synchronized (lock) {
            handles++;
        }
    }

    // Called when a handle of this queue is released (see JavaObjectHandlerMapper): the last one closes the queue.
    public void handleReleased() {
        final boolean last;
        synchronized (lock) {
            last = --handles <= 0;
        }
        if (last) {
            close();
        }
    }

    // Guarded by lock. Waits for a change, returns false on the timeout.
    private boolean await(final long timeoutMillis, final long deadline) throws InterruptedException {
        if (timeoutMillis == 0) {
            return false;
        }
        if (timeoutMillis < 0) {
            lock.wait();
            return true;
        }
        final long remaining = deadline - System.nanoTime();
        if (remaining <= 0) {
            return false;
        }
        lock.wait(remaining / 1_000_000, (int) (remaining % 1_000_000));
        return true;
    }

    // Guarded by lock.
    private List<Runnable> takeTasks() {
        final List<Runnable> taken = new ArrayList<>(tasks);
        tasks.clear();
        return taken;
    }

    private void awaitDrain() {
        final Thread current = Thread.currentThread();
        boolean interrupted = false;
        synchronized (lock) {
            while (drainThread != null && drainThread != current) {
                try {
                    lock.wait();
                } catch (final InterruptedException e) {
                    interrupted = true; // the guarantee of closeAll() is more important, keep waiting
                }
            }
        }
        if (interrupted) {
            current.interrupt();
        }
    }

    private void updateSubscriptionDropped() {
        final long total = subscription.getTotalDroppedEvents();
        synchronized (lock) {
            subscriptionDropped = total;
        }
    }

    private static void runAll(final List<Runnable> pending) {
        RuntimeException failure = null;
        for (final Runnable task : pending) {
            try {
                task.run();
            } catch (final RuntimeException e) {
                if (failure == null) {
                    failure = e;
                } else {
                    failure.addSuppressed(e);
                }
            }
        }
        if (failure != null) {
            throw failure;
        }
    }

    private final class QueueExecutor implements Executor {
        @Override
        public void execute(final Runnable task) {
            synchronized (lock) {
                if (!closed && detaching == 0) {
                    tasks.add(task);
                    lock.notifyAll();
                    return;
                }
            }
            task.run();
        }

        EventQueue queue() {
            return EventQueue.this;
        }
    }

    private final class QueueListener implements DXFeedEventListener<EventType<?>> {
        @Override
        public void eventsReceived(final List<EventType<?>> events) {
            synchronized (lock) {
                if (closed) {
                    queueDropped += events.size();
                } else if (drainThread == Thread.currentThread()) {
                    batch.addAll(events);
                } else {
                    held.addAll(events);
                    lock.notifyAll();
                }
            }
        }
    }

    private final class CloseListener implements ObservableSubscriptionChangeListener {
        @Override
        public void symbolsAdded(final Set<?> symbols) {
        }

        @Override
        public void subscriptionClosed() {
            close();
        }
    }
}
