// Copyright (c) 2026 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

package com.dxfeed.sdk.subscription;

import com.dxfeed.api.DXFeed;
import com.dxfeed.api.DXFeedEventListener;
import com.dxfeed.api.DXFeedSubscription;
import com.dxfeed.api.osub.ObservableSubscriptionChangeListener;
import com.dxfeed.event.EventType;
import com.dxfeed.sdk.javac.NativeListener;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.Executor;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * A queue of the events of its subscriptions that the host polls on its own thread (dxfg_v4_event_queue.h).
 *
 * <p>Each attached subscription gets an executor of its own that tags its "retrieve the data" tasks with the
 * attachment and puts them into the queue's FIFO, and {@link #poll} runs them on the calling thread, so QD
 * retrieves the events from its buffers only when the host polls. QD conflates the lasting events and counts the
 * events its buffers drop; the queue adds no buffer of its own. An internal listener of each subscription collects
 * the events its tasks deliver, with the tag of the attachment; the other listeners of the subscription are called
 * by the same tasks, so they run on the polling thread too. The queue-level executor ({@link #getExecutor()}) puts
 * the tasks of other consumers (models, the instrument profile collector) into the same FIFO.
 *
 * <p>A queue made by {@link #create(DXFeedSubscription)} is bound to its subscription: it closes with it, and
 * nothing else can be attached to it.
 *
 * <p>A closed queue runs every task posted to it at once, on the posting thread, and the listeners discard the
 * events and count them as dropped: QD's {@code closeAndAwaitTermination} and {@code detachSubscriptionAndClear}
 * wait until no task of the subscription is pending. The tasks of a closed queue run in the muted scope of
 * {@link NativeListener}, so they call no native function.
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

    private final boolean bound;
    private final QueueExecutor executor = new QueueExecutor();

    private final Object lock = new Object();
    // Guarded by lock.
    private final ArrayDeque<Task> tasks = new ArrayDeque<>();
    private final List<Attachment> attachments = new ArrayList<>();
    private Batch held = new Batch(); // delivered when no poll is in progress, go to the next one
    private Batch batch; // the batch of the poll in progress
    private Thread pollThread; // the thread of the poll in progress
    private boolean closed;
    private boolean wakeUp;
    private int handles;
    private long detachedDropped; // the last totals of the detached subscriptions
    private long queueDropped; // the events the queue itself dropped

    private EventQueue(final boolean bound) {
        this.bound = bound;
    }

    /**
     * Creates a queue with no subscription; {@link #attach} adds them.
     */
    public static EventQueue create() {
        synchronized (REGISTRY_LOCK) {
            return register(new EventQueue(false));
        }
    }

    /**
     * Creates a queue bound to the subscription: the subscription is attached with the tag 0, the queue closes when
     * the subscription closes, and nothing else can be attached to it.
     *
     * @throws IllegalStateException if the subscription has a queue or an executor, or QD refuses its listener (a
     *                               subscription attached to a feed that already has symbols).
     */
    public static EventQueue create(final DXFeedSubscription<?> subscription) {
        synchronized (REGISTRY_LOCK) {
            checkAttachable(subscription);
            final EventQueue queue = register(new EventQueue(true));
            try {
                queue.install(subscription, 0);
            } catch (final RuntimeException | Error e) {
                queue.close();
                throw e;
            }
            return queue;
        }
    }

    /**
     * Attaches the subscription to the queue: its events go to the polls of the queue with the tag.
     * A closed subscription, or a subscription attached to a closed queue, is attached as detached: its tasks run at
     * once and its events are discarded and counted. A subscription leaves the queue only by being closed.
     *
     * @throws IllegalStateException if the queue is bound to a subscription, the subscription has a queue or an
     *                               executor, or QD refuses its listener (a subscription attached to a feed that
     *                               already has symbols).
     */
    public void attach(final DXFeedSubscription<?> subscription, final long tag) {
        if (bound) {
            throw new IllegalStateException("The event queue belongs to its subscription, nothing can be attached");
        }
        synchronized (REGISTRY_LOCK) {
            checkAttachable(subscription);
            install(subscription, tag);
        }
    }

    /**
     * @return the queue-level executor: the tasks posted to it run inside a poll, the ones posted to a closed queue
     *         run at once in the muted scope.
     * @throws IllegalStateException if the queue is bound to a subscription.
     */
    public Executor getExecutor() {
        if (bound) {
            throw new IllegalStateException("The event queue belongs to its subscription, it has no executor to share");
        }
        return executor;
    }

    /**
     * @return the queue the subscription is attached to or {@code null} if it is attached to none.
     */
    public static EventQueue of(final DXFeedSubscription<?> subscription) {
        final Attachment attachment = attachmentOf(subscription);
        return attachment != null ? attachment.queue() : null;
    }

    // The attachment of the subscription, or null if it is not attached to a queue.
    private static Attachment attachmentOf(final DXFeedSubscription<?> subscription) {
        final Executor current = subscription.getExecutor();
        return current instanceof AttachmentExecutor ? ((AttachmentExecutor) current).attachment : null;
    }

    /**
     * @return the queue whose handles the handle of the object counts toward: the queue itself or its queue-level
     *         executor; {@code null} for any other object (see JavaObjectHandlerMapper).
     */
    public static EventQueue countedBy(final Object javaObject) {
        if (javaObject instanceof EventQueue) {
            return (EventQueue) javaObject;
        }
        return javaObject instanceof QueueExecutor ? ((QueueExecutor) javaObject).queue() : null;
    }

    /**
     * Checks an executor given to a native function that accepts one.
     *
     * @return the executor.
     * @throws IllegalStateException if it is the executor of a subscription attached to a queue: its tasks would be
     *                               taken for the subscription's tasks.
     */
    public static Executor checkExecutor(final Executor executor) {
        if (executor instanceof AttachmentExecutor) {
            throw new IllegalStateException("The executor of a subscription attached to an event queue cannot be "
                    + "used elsewhere");
        }
        return executor;
    }

    /**
     * Sets the executor of a subscription (or of its subscription controller) unless the subscription is attached to
     * a queue.
     *
     * @throws IllegalStateException if the subscription is attached to a queue or the executor is the one of an
     *                               attached subscription.
     */
    public static void setExecutor(final Supplier<Executor> current, final Consumer<Executor> setter,
            final Executor executor) {
        synchronized (REGISTRY_LOCK) {
            if (current.get() instanceof AttachmentExecutor) {
                throw new IllegalStateException("The subscription is attached to an event queue, its executor cannot "
                        + "be changed");
            }
            setter.accept(checkExecutor(executor));
        }
    }

    /**
     * Calls {@link DXFeed#detachSubscriptionAndClear} without waiting for a poll: the tasks of the subscription
     * pending in the queue and the ones posted while QD detaches run at once, their events go to the poll in
     * progress or to the next one. The tasks of the other owners stay in the queue.
     */
    public static void detachSubscriptionAndClear(final DXFeed feed, final DXFeedSubscription<?> subscription) {
        final Attachment attachment = attachmentOf(subscription);
        if (attachment == null) {
            feed.detachSubscriptionAndClear(subscription);
            return;
        }

        final EventQueue queue = attachment.queue();
        final List<Task> pending;
        final boolean muted;
        synchronized (queue.lock) {
            attachment.detaching++;
            pending = queue.takeTasks(attachment);
            muted = queue.closed;
        }
        try {
            runAll(pending, muted);
            feed.detachSubscriptionAndClear(subscription);
        } finally {
            synchronized (queue.lock) {
                attachment.detaching--;
            }
            queue.updateDropped(attachment);
        }
    }

    /**
     * Closes all the queues of the isolate, the queues created afterwards are closed from the start.
     * Waits for the polls in progress on the other threads.
     */
    public static void closeAll() {
        final List<EventQueue> queues;
        synchronized (REGISTRY_LOCK) {
            allClosed = true;
            queues = new ArrayList<>(QUEUES);
        }
        // A task that fails while its queue closes does not leave the other queues open.
        RuntimeException failure = null;
        for (final EventQueue queue : queues) {
            try {
                queue.close();
            } catch (final RuntimeException e) {
                if (failure == null) {
                    failure = e;
                } else {
                    failure.addSuppressed(e);
                }
            }
        }
        for (final EventQueue queue : queues) {
            queue.awaitPoll();
        }
        if (failure != null) {
            throw failure;
        }
    }

    /**
     * Waits for events and returns them; see {@link #pollTagged}.
     */
    public List<EventType<?>> poll(final long requestedTimeoutMillis, final int limit) throws InterruptedException {
        final Batch result = pollTagged(requestedTimeoutMillis, limit);
        return result == null ? null : result.events;
    }

    /**
     * Waits for events and returns them with their tags.
     *
     * <p>The polls of one queue are serialized: a poll waits for the one in progress on another thread.
     * Once a poll has events or has run a task of the queue-level executor, it runs only the tasks pending at that
     * moment. A poll that ran a task of the queue-level executor and has no events returns at once.
     *
     * @param requestedTimeoutMillis 0 returns at once, a negative value or one over
     *                               {@code Long.MAX_VALUE / 1_000_000} waits until events arrive, a wake-up or a close.
     * @param limit                  the number of events after which no more tasks are run (a batch may exceed it),
     *                               0 for no limit.
     * @return the events, an empty batch if there were none before the timeout or a wake-up, or if only the tasks of
     *         the queue-level executor ran, {@code null} if the queue is closed. Once it has events, a poll returns
     *         them also if the queue is closed meanwhile.
     * @throws IllegalStateException if called from a task or a listener during a poll of this queue.
     */
    public Batch pollTagged(final long requestedTimeoutMillis, final int limit) throws InterruptedException {
        // A timeout that does not fit in nanoseconds (over 292 years) is an infinite one.
        final long timeoutMillis = requestedTimeoutMillis > MAX_TIMEOUT_MILLIS ? -1 : requestedTimeoutMillis;
        final long deadline = timeoutMillis > 0 ? System.nanoTime() + timeoutMillis * 1_000_000 : 0;
        final Thread current = Thread.currentThread();
        synchronized (lock) {
            if (pollThread == current) {
                throw new IllegalStateException("The event queue is polled from a listener of its own poll");
            }
            while (true) {
                if (closed) {
                    wakeUp = false;
                    return null;
                }
                if (pollThread == null) {
                    break;
                }
                // The wake-up stays for the poll in progress.
                if (!await(timeoutMillis, deadline)) {
                    return new Batch();
                }
            }
            pollThread = current;
            // The events delivered meanwhile on other threads go to this batch, after the held ones.
            batch = held;
            held = new Batch();
        }

        try {
            // Once there are events, only the tasks pending at that moment are run: QD posts a new task while its
            // buffer has data, so a poll without a limit would not return under a steady flow. The same holds
            // after the first task of the queue-level executor: its owners may post tasks endlessly.
            int remainingTasks = -1;
            boolean ranQueueTask = false;
            while (true) {
                final Task task;
                synchronized (lock) {
                    if (!batch.isEmpty() || ranQueueTask) {
                        if (remainingTasks < 0) {
                            remainingTasks = tasks.size();
                        }
                        if (remainingTasks == 0 || limit > 0 && batch.size() >= limit) {
                            return endPoll();
                        }
                        remainingTasks--;
                    }
                    task = tasks.poll();
                    if (task == null) {
                        // With events, a close or a detach on another thread took the tasks counted in
                        // remainingTasks.
                        if (!batch.isEmpty() || ranQueueTask) {
                            return endPoll();
                        }
                        if (closed) {
                            endPoll();
                            return null;
                        }
                        if (wakeUp || !await(timeoutMillis, deadline)) {
                            return endPoll();
                        }
                        continue;
                    }
                }
                task.runnable.run();
                if (task.owner == null) {
                    ranQueueTask = true;
                } else {
                    updateDropped(task.owner);
                }
            }
        } catch (final RuntimeException | Error e) {
            synchronized (lock) {
                if (pollThread == current) {
                    queueDropped += batch.size(); // retrieved, but not returned
                }
            }
            throw e;
        } finally {
            synchronized (lock) {
                if (pollThread == current) {
                    endPoll();
                }
            }
        }
    }

    /**
     * Counts a batch returned by {@link #poll} as dropped: the caller failed to deliver it (e.g. to convert it to the
     * C structures).
     */
    public void dropBatch(final List<EventType<?>> events) {
        synchronized (lock) {
            queueDropped += events.size();
        }
    }

    /**
     * Makes the poll in progress, or the next one if none is waiting, return no events.
     */
    public void wakeUp() {
        synchronized (lock) {
            wakeUp = true;
            lock.notifyAll();
        }
    }

    /**
     * Closes the queue: wakes the polls, runs the pending tasks of every owner on this thread in the muted scope,
     * discarding their events. The queue stays the executor of its subscriptions.
     */
    public void close() {
        final List<Task> pending;
        synchronized (lock) {
            closed = true;
            lock.notifyAll();
            queueDropped += held.size();
            held.clear();
            pending = new ArrayList<>(tasks);
            tasks.clear();
        }
        // Stays in QUEUES (weak): removing it here would take REGISTRY_LOCK under the subscription's lock
        // (subscriptionClosed), the reverse of the order in create().
        try {
            runAll(pending, true);
        } finally {
            final List<Attachment> live;
            synchronized (lock) {
                live = new ArrayList<>(attachments);
            }
            for (final Attachment attachment : live) {
                updateDropped(attachment);
            }
        }
    }

    public boolean isClosed() {
        synchronized (lock) {
            return closed;
        }
    }

    /**
     * @return the total of dropped events of the attached subscriptions as of the last time their tasks ran (the
     *         last totals for the detached ones), plus the events the queue dropped itself (a failed conversion, the
     *         events of a closed queue or of a closed subscription). Never decreases.
     */
    public long getDroppedEvents() {
        synchronized (lock) {
            long total = detachedDropped + queueDropped;
            for (final Attachment attachment : attachments) {
                total += attachment.dropped;
            }
            return total;
        }
    }

    // Called when a handle of this queue or of its queue-level executor is created (see JavaObjectHandlerMapper).
    public void handleCreated() {
        synchronized (lock) {
            handles++;
        }
    }

    // Called when a handle of this queue or of its queue-level executor is released (see JavaObjectHandlerMapper):
    // the last one closes the queue.
    public void handleReleased() {
        final boolean last;
        synchronized (lock) {
            last = --handles <= 0;
        }
        if (last) {
            close();
        }
    }

    // Guarded by REGISTRY_LOCK.
    private static EventQueue register(final EventQueue queue) {
        if (allClosed) {
            queue.closed = true;
        } else {
            QUEUES.add(queue);
        }
        return queue;
    }

    // Guarded by REGISTRY_LOCK.
    private static void checkAttachable(final DXFeedSubscription<?> subscription) {
        if (subscription.getExecutor() instanceof AttachmentExecutor) {
            throw new IllegalStateException("The subscription is already attached to an event queue");
        }
        if (subscription.getExecutor() != null) {
            throw new IllegalStateException("The subscription has an executor, attaching it to an event queue needs "
                    + "its own");
        }
    }

    // Guarded by REGISTRY_LOCK. Installs the listeners and the executor first, then registers the attachment under
    // the queue's lock: the queue's lock is never held across a call into the subscription (subscriptionClosed takes
    // it under the subscription's lock).
    @SuppressWarnings({"unchecked", "rawtypes"})
    private void install(final DXFeedSubscription<?> subscription, final long tag) {
        final Attachment attachment = new Attachment(subscription, tag);
        final DXFeedSubscription raw = subscription;
        boolean listenerAdded = false;
        boolean executorSet = false;
        try {
            raw.addEventListener(attachment.listener);
            listenerAdded = true;
            subscription.setExecutor(attachment.executor);
            executorSet = true;
            subscription.addChangeListener(attachment.closeListener);
        } catch (final RuntimeException | Error e) {
            if (executorSet) {
                subscription.setExecutor(null);
            }
            if (listenerAdded) {
                raw.removeEventListener(attachment.listener);
            }
            throw e;
        }

        // A subscription closed before its close listener was added is caught here, one closed after it is detached
        // by the listener.
        final boolean subscriptionClosed = subscription.isClosed();
        synchronized (lock) {
            if (closed || subscriptionClosed || attachment.detached) {
                attachment.detached = true;
                // A closed subscription calls no close listener added after its close: its bound queue closes here.
                closed |= bound;
            } else {
                attachments.add(attachment);
            }
        }
    }

    // Detaches the attachment of a closed subscription: runs its pending tasks on this thread discarding their
    // events, its later tasks run at once. Called under the subscription's lock: takes no REGISTRY_LOCK and runs
    // no task of another owner.
    private void detach(final Attachment attachment) {
        final List<Task> pending;
        final boolean muted;
        synchronized (lock) {
            if (attachment.detached) {
                return;
            }
            attachment.detached = true;
            pending = takeTasks(attachment);
            muted = closed;
        }
        try {
            runAll(pending, muted);
        } finally {
            final long total = attachment.subscription.getTotalDroppedEvents();
            synchronized (lock) {
                if (attachments.remove(attachment)) {
                    detachedDropped += Math.max(attachment.dropped, total);
                }
            }
            if (bound) {
                close();
            }
        }
    }

    // Guarded by lock. Ends the poll in progress, returns its batch.
    private Batch endPoll() {
        final Batch result = batch;
        batch = null;
        pollThread = null;
        wakeUp = false;
        lock.notifyAll();
        return result;
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

    // Guarded by lock. Takes the pending tasks of the owner (null: of the queue-level executor).
    private List<Task> takeTasks(final Attachment owner) {
        final List<Task> taken = new ArrayList<>();
        for (final Iterator<Task> it = tasks.iterator(); it.hasNext(); ) {
            final Task task = it.next();
            if (task.owner == owner) {
                taken.add(task);
                it.remove();
            }
        }
        return taken;
    }

    private void awaitPoll() {
        final Thread current = Thread.currentThread();
        boolean interrupted = false;
        synchronized (lock) {
            while (pollThread != null && pollThread != current) {
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

    private void updateDropped(final Attachment attachment) {
        final long total = attachment.subscription.getTotalDroppedEvents();
        synchronized (lock) {
            // A detached attachment keeps the total it had when it left.
            if (!attachment.detached && total > attachment.dropped) {
                attachment.dropped = total;
            }
        }
    }

    private static void runAll(final List<Task> pending, final boolean muted) {
        RuntimeException failure = null;
        for (final Task task : pending) {
            try {
                run(task.runnable, muted);
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

    private static void run(final Runnable task, final boolean muted) {
        if (muted) {
            NativeListener.runMuted(task);
        } else {
            task.run();
        }
    }

    /**
     * The events of a poll and their tags: {@code tag(i)} is the tag of the attachment that delivered
     * {@code events().get(i)}.
     */
    public static final class Batch {
        private final List<EventType<?>> events = new ArrayList<>();
        private long[] tags = new long[16];

        public List<EventType<?>> events() {
            return events;
        }

        public long tag(final int index) {
            return tags[Objects.checkIndex(index, events.size())];
        }

        public int size() {
            return events.size();
        }

        public boolean isEmpty() {
            return events.isEmpty();
        }

        private void add(final List<? extends EventType<?>> delivered, final long tag) {
            final int from = events.size();
            ensureCapacity(from + delivered.size());
            events.addAll(delivered);
            Arrays.fill(tags, from, events.size(), tag);
        }

        private void clear() {
            events.clear();
        }

        private void ensureCapacity(final int capacity) {
            if (capacity > tags.length) {
                tags = Arrays.copyOf(tags, Math.max(capacity, tags.length * 2));
            }
        }
    }

    // A task and its owner: an attachment, or null for the queue-level executor.
    private static final class Task {
        final Attachment owner;
        final Runnable runnable;

        Task(final Attachment owner, final Runnable runnable) {
            this.owner = owner;
            this.runnable = runnable;
        }
    }

    // A subscription attached to the queue with its tag.
    private final class Attachment {
        final DXFeedSubscription<?> subscription;
        final long tag;
        final AttachmentExecutor executor = new AttachmentExecutor(this);
        final AttachmentListener listener = new AttachmentListener(this);
        final CloseListener closeListener = new CloseListener(this);
        // Guarded by lock.
        int detaching;
        boolean detached;
        long dropped; // QD's total as of the last time the tasks ran

        Attachment(final DXFeedSubscription<?> subscription, final long tag) {
            this.subscription = subscription;
            this.tag = tag;
        }

        EventQueue queue() {
            return EventQueue.this;
        }
    }

    // The executor of an attached subscription: tags its tasks with the attachment.
    private final class AttachmentExecutor implements Executor {
        final Attachment attachment;

        AttachmentExecutor(final Attachment attachment) {
            this.attachment = attachment;
        }

        @Override
        public void execute(final Runnable task) {
            final boolean muted;
            synchronized (lock) {
                if (!closed && !attachment.detached && attachment.detaching == 0) {
                    tasks.add(new Task(attachment, task));
                    lock.notifyAll();
                    return;
                }
                muted = closed;
            }
            run(task, muted);
        }
    }

    // The queue-level executor: its tasks belong to the queue itself.
    private final class QueueExecutor implements Executor {
        @Override
        public void execute(final Runnable task) {
            synchronized (lock) {
                if (!closed) {
                    tasks.add(new Task(null, task));
                    lock.notifyAll();
                    return;
                }
            }
            NativeListener.runMuted(task);
        }

        EventQueue queue() {
            return EventQueue.this;
        }
    }

    private final class AttachmentListener implements DXFeedEventListener<EventType<?>> {
        final Attachment attachment;

        AttachmentListener(final Attachment attachment) {
            this.attachment = attachment;
        }

        @Override
        public void eventsReceived(final List<EventType<?>> events) {
            synchronized (lock) {
                if (closed || attachment.detached) {
                    queueDropped += events.size();
                } else if (batch != null) {
                    // On the polling thread or on a thread that runs a task of a detach meanwhile.
                    batch.add(events, attachment.tag);
                    lock.notifyAll();
                } else {
                    held.add(events, attachment.tag);
                    lock.notifyAll();
                }
            }
        }
    }

    private final class CloseListener implements ObservableSubscriptionChangeListener {
        final Attachment attachment;

        CloseListener(final Attachment attachment) {
            this.attachment = attachment;
        }

        @Override
        public void symbolsAdded(final Set<?> symbols) {
        }

        @Override
        public void subscriptionClosed() {
            detach(attachment);
        }
    }
}
