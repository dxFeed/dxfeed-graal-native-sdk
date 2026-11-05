// Copyright (c) 2026 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

package com.dxfeed.sdk.connector;

import com.devexperts.qd.qtp.MessageConnector;
import com.devexperts.util.LogUtil;
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

    private static final ThreadLocal<Pending> PENDING = new ThreadLocal<>();

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
     * Takes and clears the pending record of the current thread.
     *
     * @return the record, or {@code null} when there is none
     */
    static Pending take() {
        final Pending pending = PENDING.get();

        if (pending != null) {
            PENDING.remove();
        }

        return pending;
    }

    @Override
    public void publish(final LogRecord record) {
        // Called from QD's logging call on its connection thread: nothing may escape into QD.
        try {
            final Throwable thrown = record.getThrown();

            if (thrown != null) {
                PENDING.set(new Pending(record.getLoggerName(), record.getMillis(),
                        ConnectionErrorClassifier.classify(thrown), thrown.getClass().getName(),
                        bound(thrown.getMessage())));
            } else if (HEARTBEAT_LOGGER.equals(record.getLoggerName())) {
                PENDING.set(new Pending(record.getLoggerName(), record.getMillis(),
                        ConnectionError.CATEGORY_HEARTBEAT_TIMEOUT, null, bound(record.getMessage())));
            }
        } catch (final RuntimeException e) {
            PENDING.remove(); // The record is not captured.
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
