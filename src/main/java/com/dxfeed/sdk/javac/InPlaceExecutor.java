// Copyright (c) 2026 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

package com.dxfeed.sdk.javac;

import com.devexperts.logging.Logging;
import java.util.concurrent.Executor;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * The executor of {@code dxfg_ExecutorBaseOnConcurrentLinkedQueue_new}: it only queues the tasks, and they run on the
 * thread that calls {@link #processAllPendingTasks()}.
 */
public final class InPlaceExecutor implements Executor {

    private static final Logging log = Logging.getLogging(InPlaceExecutor.class);

    private static final Runnable WAKE_UP = () -> {
    };

    private static final long DONE_CHECK_INTERVAL_MS = 50;

    private final LinkedBlockingQueue<Runnable> taskQueue = new LinkedBlockingQueue<>();

    @Override
    public void execute(final Runnable command) {
        taskQueue.add(command);
    }

    public void processAllPendingTasks() {
        Runnable task;
        while ((task = taskQueue.poll()) != null) {
            task.run();
        }
    }

    /**
     * Runs the tasks on the calling thread as they come until {@code done} completes. Whoever completes it should call
     * {@link #wakeUp()} afterwards, so that this method returns at once. Another thread may run the queued tasks at the
     * same time in {@link #processAllPendingTasks()} and take that wake-up task, so the wait for a task is bounded and
     * {@code done} is checked again after it. A task that throws is logged and does not stop the loop, since
     * {@code done} may depend on the tasks after it.
     */
    public void processTasksUntilDone(final Future<?> done) throws InterruptedException {
        while (!done.isDone()) {
            final Runnable task = taskQueue.poll(DONE_CHECK_INTERVAL_MS, TimeUnit.MILLISECONDS);
            if (task == null) {
                continue;
            }
            try {
                task.run();
            } catch (final RuntimeException e) {
                log.error("A task of the in-place executor failed", e);
            }
        }
    }

    /**
     * Wakes {@link #processTasksUntilDone} up. The empty task it queues may also run later in
     * {@link #processAllPendingTasks()}.
     */
    public void wakeUp() {
        taskQueue.add(WAKE_UP);
    }
}
