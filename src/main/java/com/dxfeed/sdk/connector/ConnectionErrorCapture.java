// Copyright (c) 2026 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

package com.dxfeed.sdk.connector;

import com.devexperts.qd.qtp.MessageConnector;
import com.devexperts.qd.qtp.QTPWorkerThread;
import com.devexperts.util.LogUtil;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.function.Consumer;
import java.util.function.ToLongFunction;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

/**
 * Captures the connection errors from QD's own log records: a JUL handler on QD's connector, TLS and heartbeat
 * loggers keeps, per thread, the last record with an exception or the last heartbeat timeout as copied values. The
 * connector monitor takes it in {@code stateChanged} on the same thread (QD logs the failure and then notifies the
 * listeners on one thread) and attributes it to the connector.
 *
 * <p>A socket connection of QD has two threads, {@code <address>-Reader} and {@code <address>-Writer}, and either of
 * them may close it: when the thread that logged the failure closes it after the other one, only the other one
 * notifies the listeners. So a thread of a socket connection without a record of its own takes the fresh record of
 * the other thread of its connection: the thread of the paired name that QD created right before or after it. The
 * other thread may even log the failure after the notification (a failed
 * {@code SSLEngine} closes itself before it throws, and the other thread closes the connection when it sees that):
 * then a thread that closed its connection and notifies the listeners without a record waits for a short time
 * ({@link #takeOrAwait}), and the record of the other thread goes to the connector at once.
 *
 * <p>The handler is added once per isolate, at the first endpoint, when QD's logging is configured already. It does
 * not touch the root logger and its handlers, so the user's output stays as it is. A capture logger without a level
 * of its own whose effective level would drop the records gets the level it needs from the SDK; a level the user sets
 * on it wins.
 */
final class ConnectionErrorCapture extends Handler {

    static final String CONNECTOR_LOGGER = MessageConnector.class.getName();
    static final String TLS_LOGGER = "com.devexperts.connector.codec.ssl";
    static final String HEARTBEAT_LOGGER = "com.devexperts.qd.qtp.MessageAdapterConnection";

    // The bound of a copied message.
    static final int MAX_MESSAGE_LENGTH = 256;

    // The bound of the age of a record that the other thread of a socket connection takes, and of the wait for a
    // record of the other thread: the failure of one thread and the notification by the other one follow each other
    // within milliseconds.
    static final long PAIRED_MAX_AGE_MILLIS = 2_000;

    // The suffixes of the names of QD's socket threads, after the address of the connection.
    static final String READER_SUFFIX = "-Reader";
    static final String WRITER_SUFFIX = "-Writer";

    // The pending records by thread, guarded by itself. Weak: a thread that ends with a record no one took drops it.
    private static final Map<Thread, Pending> PENDING = new WeakHashMap<>();

    // The socket threads that closed their connection and notified the listeners without a record, guarded by PENDING.
    private static final Map<Thread, Waiting> WAITING = new WeakHashMap<>();

    // The capture loggers, strongly referenced so that JUL keeps their handler and levels; null until install().
    private static Logger[] loggers;

    // The levels each capture logger needs: QD logs connection errors as ERROR, the heartbeat timeout as INFO.
    private static final Level[] NEEDED_LEVELS = {Level.SEVERE, Level.SEVERE, Level.INFO};

    // The levels the SDK set on the capture loggers (null where it set none), to tell them from the user's levels.
    private static final Level[] SDK_LEVELS = new Level[NEEDED_LEVELS.length];

    private ConnectionErrorCapture() {
        setLevel(Level.ALL);
    }

    /**
     * The copied values of a captured record.
     */
    record Pending(String loggerName, long time, int category, String exceptionClassName, String message) {
    }

    // The wait of a socket thread for a record of the other thread of its connection.
    private record Waiting(long time, Consumer<Pending> sink) {
    }

    /**
     * Adds the handler to the capture loggers; later calls do nothing.
     */
    static synchronized void install() {
        if (loggers != null) {
            return;
        }

        final ConnectionErrorCapture handler = new ConnectionErrorCapture();

        loggers = new Logger[] {
                Logger.getLogger(CONNECTOR_LOGGER), Logger.getLogger(TLS_LOGGER), Logger.getLogger(HEARTBEAT_LOGGER)
        };

        for (final Logger logger : loggers) {
            logger.addHandler(handler);
        }

        updateLevels();
    }

    /**
     * Sets the levels of the capture loggers anew after the user's levels changed: a capture logger without a level of
     * its own, or with the level the SDK set, gets the level it needs if its effective level would drop the records,
     * and no level of its own otherwise. A logger with a level the user set stays as it is.
     */
    static synchronized void updateLevels() {
        if (loggers == null) {
            return;
        }

        for (int i = 0; i < loggers.length; i++) {
            final Logger logger = loggers[i];
            final Level own = logger.getLevel();

            if (own != null && own != SDK_LEVELS[i]) {
                continue;
            }

            if (own != null) {
                logger.setLevel(null);
            }

            if (logger.isLoggable(NEEDED_LEVELS[i])) {
                SDK_LEVELS[i] = null;
            } else {
                logger.setLevel(NEEDED_LEVELS[i]);
                SDK_LEVELS[i] = NEEDED_LEVELS[i];
            }
        }
    }

    /**
     * Takes and clears the pending record of the current thread or, when it has none and is a thread of a socket
     * connection, the record of the other thread of the connection that is at most {@link #PAIRED_MAX_AGE_MILLIS} old.
     *
     * @return the record, or {@code null} when there is none
     */
    static Pending take() {
        return takeOrAwait(null);
    }

    /**
     * Takes the record as {@link #take()} does; when there is none, the sink is not {@code null} and the current thread
     * is a thread of a socket connection, the next record that the other thread of the connection captures within
     * {@link #PAIRED_MAX_AGE_MILLIS} goes to the sink, on that thread, instead of waiting for a notification. Atomic
     * with the capture: a record that the other thread captures meanwhile is either returned or goes to the sink.
     *
     * @param sink the consumer of a later record, or {@code null} for no wait
     * @return the record, or {@code null} when there is none
     */
    static Pending takeOrAwait(final Consumer<Pending> sink) {
        final Thread current = Thread.currentThread();
        final boolean await = sink != null && pairedName(current.getName()) != null;

        synchronized (PENDING) {
            if (!PENDING.isEmpty()) {
                final Pending own = PENDING.remove(current);
                final Pending taken = own != null ? own : removePaired(PENDING, current, Pending::time);

                if (taken != null) {
                    return taken;
                }
            }

            if (await) {
                final long now = System.currentTimeMillis();

                WAITING.values().removeIf(waiting -> now - waiting.time() > PAIRED_MAX_AGE_MILLIS);
                WAITING.put(current, new Waiting(now, sink));
            }

            return null;
        }
    }

    /**
     * @return whether the current thread is a QD worker thread that is closed: QD closes both threads of a socket
     * connection when it closes the connection, so the thread that closed it notifies the listeners on a closed thread,
     * and the threads of the next connection, which notify them as well, are not closed
     */
    static boolean isClosedWorkerThread() {
        if (!(Thread.currentThread() instanceof QTPWorkerThread worker)) {
            return false;
        }

        // isClosed() clears the interrupt flag; QD interrupts a closed worker thread and relies on the flag, so it is
        // set again.
        try {
            if (!worker.isClosed()) {
                return false;
            }

            worker.interrupt();
            return true;
        } catch (final InterruptedException e) {
            worker.interrupt(); // Interrupted and not closed: the flag stays.
            return false;
        }
    }

    // Guarded by PENDING. Removes the fresh value of the other thread of the current thread's socket connection. The
    // names of the threads tell only the address of the connection, and a stopped connection may leave a record behind
    // (its thread logs a failure after the stop and notifies no one), so the other thread is told by its id as well:
    // QD creates the reader of a connection and then its writer, so the id of the writer follows the id of the reader.
    // When another thread is created between them, the threads of the connection do not pair.
    private static <V> V removePaired(final Map<Thread, V> values, final Thread current, final ToLongFunction<V> time) {
        if (values.isEmpty()) {
            return null;
        }

        final String name = current.getName();
        final String pairedName = pairedName(name);

        if (pairedName == null) {
            return null;
        }

        final long pairedId = current.threadId() + (name.endsWith(READER_SUFFIX) ? 1 : -1);
        final long now = System.currentTimeMillis();

        for (final Map.Entry<Thread, V> entry : values.entrySet()) {
            final Thread thread = entry.getKey();

            if (thread != null && thread.threadId() == pairedId && pairedName.equals(thread.getName())
                    && now - time.applyAsLong(entry.getValue()) <= PAIRED_MAX_AGE_MILLIS) {
                return values.remove(thread);
            }
        }

        return null;
    }

    /**
     * @return the name of the other thread of a QD socket connection, or {@code null} if the name is not of one
     */
    static String pairedName(final String name) {
        if (name.endsWith(READER_SUFFIX)) {
            return name.substring(0, name.length() - READER_SUFFIX.length()) + WRITER_SUFFIX;
        }

        if (name.endsWith(WRITER_SUFFIX)) {
            return name.substring(0, name.length() - WRITER_SUFFIX.length()) + READER_SUFFIX;
        }

        return null;
    }

    // Keeps the record of the current thread, or hands it to the other thread of its socket connection that waits.
    private static void capture(final Pending pending) {
        final Thread current = Thread.currentThread();
        final Waiting waiting;

        synchronized (PENDING) {
            waiting = removePaired(WAITING, current, Waiting::time);

            if (waiting == null) {
                PENDING.put(current, pending);
                return;
            }

            PENDING.remove(current);
        }

        waiting.sink().accept(pending);
    }

    @Override
    public void publish(final LogRecord record) {
        // Called from QD's logging call on its connection thread: nothing may escape into QD.
        try {
            final Throwable thrown = record.getThrown();

            if (thrown != null) {
                capture(new Pending(record.getLoggerName(), record.getMillis(),
                        ConnectionErrorClassifier.classify(thrown), thrown.getClass().getName(),
                        bound(thrown.getMessage())));
            } else if (HEARTBEAT_LOGGER.equals(record.getLoggerName())) {
                capture(new Pending(record.getLoggerName(), record.getMillis(),
                        ConnectionError.CATEGORY_HEARTBEAT_TIMEOUT, null, bound(record.getMessage())));
            }
        } catch (final RuntimeException e) {
            synchronized (PENDING) {
                PENDING.remove(Thread.currentThread()); // The record is not captured.
            }
        }
    }

    @Override
    public void flush() {
    }

    @Override
    public void close() {
    }

    static String bound(final String message) {
        if (message == null) {
            return null;
        }

        final String masked = LogUtil.hideCredentials(message);

        return masked.length() <= MAX_MESSAGE_LENGTH ? masked : masked.substring(0, MAX_MESSAGE_LENGTH);
    }
}
