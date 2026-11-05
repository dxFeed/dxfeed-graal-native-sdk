// Copyright (c) 2025 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

package com.devexperts.logging;

import com.dxfeed.sdk.connector.ConnectionErrors;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.ConsoleHandler;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

/**
 * Logging implementation that allows to intercept logged data, configure the logging level, and set it on the fly
 */
public class InterceptableLogging extends DefaultLogging {

    /// The name of the property that allows to set the logging level to console or to listener or to log file.
    public static final String LOG_LEVEL = "log.level";

    /// The name of the property that allows to set the logging level to an err file.
    public static final String ERR_LEVEL = "err.level";

    /// Logging level to console or to listener by default: the default level of the JUL root logger, which limited the
    /// output before the levels also set the level of the root logger.
    public static final String DEFAULT_LOG_LEVEL = Level.INFO.toString();

    /// Logging level to err file by default.
    public static final String DEFAULT_ERR_LEVEL = Level.WARNING.toString();

    static final Object lock = new Object();
    static InterceptableLoggingListener listener_;

    // Guarded by lock: the root handlers are the ones this class configured (and so have the user's levels), not
    // the ones of another logging implementation.
    static boolean configured_;

    static Level getLogLevel(String propertyName, String defaultValue) {
        try {
            var logLevel = System.getProperty(propertyName, defaultValue);

            if (logLevel.equalsIgnoreCase("ERROR") || logLevel.equalsIgnoreCase("ERR")) {
                return Level.SEVERE;
            }

            if (logLevel.equalsIgnoreCase("WARN")) {
                return Level.WARNING;
            }

            if (logLevel.equalsIgnoreCase("DEBUG")) {
                return Level.FINE;
            }

            if (logLevel.equalsIgnoreCase("TRACE")) {
                return Level.FINEST;
            }

            return Level.parse(System.getProperty(propertyName, defaultValue));
        } catch (IllegalArgumentException e) {
            return Level.parse(defaultValue);
        }
    }

    /**
     * Sets the logging listener. This method will toggle logging. Messages will not be logged to console and files.
     *
     * @param listener The logging listener.
     */
    public static void setListener(InterceptableLoggingListener listener) {
        synchronized (lock) {
            if (listener_ == null && listener != null) {
                listener_ = listener;

                Logger root = Logger.getLogger("");

                for (Handler handler : root.getHandlers()) {
                    root.removeHandler(handler);
                }

                Handler handler = new LoggingHandler();
                handler.setFormatter(new LogFormatter());
                handler.setLevel(getLogLevel(LOG_LEVEL, DEFAULT_LOG_LEVEL));
                root.addHandler(handler);
                configured_ = true;
                updateRootLevel(root);
            }
        }
    }

    /**
     * Sets the logging level for the listener or console or log file.
     *
     * @param level The new logging level.
     */
    public static void setLogLevel(Level level) {
        synchronized (lock) {
            Logger root = Logger.getLogger("");
            var handlers = root.getHandlers();

            if (handlers.length > 0) {
                handlers[0].setLevel(level); // console or listener, or log file
                updateRootLevel(root);
            }
        }
    }

    /**
     * Sets the logging level for the err file.
     *
     * @param level The new logging level.
     */
    public static void setErrLevel(Level level) {
        synchronized (lock) {
            Logger root = Logger.getLogger("");
            var handlers = root.getHandlers();

            if (handlers.length > 1) {
                handlers[1].setLevel(level); // err file
                updateRootLevel(root);
            }
        }
    }

    /**
     * Sets the level of the root logger to the most verbose level of the root handlers, so that the records below it
     * are not created at all and the records of a more verbose handler level are; then the levels of the loggers that
     * the SDK's connection error capture needs. Only for the handlers this class configured: the handlers of another
     * logging implementation keep the root logger as it is (e.g. QD's console handler has the level ALL).
     */
    static void updateRootLevel(Logger root) {
        if (!configured_) {
            return;
        }

        Level level = null;

        for (Handler handler : root.getHandlers()) {
            if (level == null || handler.getLevel().intValue() < level.intValue()) {
                level = handler.getLevel();
            }
        }

        if (level != null) {
            root.setLevel(level);
        }

        ConnectionErrors.loggingLevelsChanged();
    }

    @Override
    Map<String, Exception> configureLogFile(final String log_file) {
        synchronized (lock) {
            Logger root = Logger.getLogger("");
            Map<String, Exception> errors = new LinkedHashMap<>();

            try {
                for (Handler handler : root.getHandlers()) {
                    root.removeHandler(handler);
                }

                // configure "log" file or console
                Handler handler = null;
                if (log_file != null) {
                    try {
                        handler = new FileHandler(log_file,
                                getLimit(Logging.LOG_MAX_FILE_SIZE_PROPERTY, errors),
                                2, true);
                    } catch (IOException e) {
                        errors.put(log_file, e);
                    }
                }
                if (handler == null) {
                    handler = new ConsoleHandler();
                }
                handler.setFormatter(new LogFormatter());
                handler.setLevel(getLogLevel(LOG_LEVEL, DEFAULT_LOG_LEVEL));
                root.addHandler(handler);

                // configure "err" file
                String err_file = getProperty(Logging.ERR_FILE_PROPERTY, null);
                if (err_file != null) {
                    try {
                        handler = new FileHandler(err_file,
                                getLimit(Logging.ERR_MAX_FILE_SIZE_PROPERTY, errors),
                                2, true);
                        handler.setFormatter(new LogFormatter());
                        handler.setLevel(getLogLevel(ERR_LEVEL, DEFAULT_ERR_LEVEL));
                        root.addHandler(handler);
                    } catch (IOException e) {
                        errors.put(err_file, e);
                    }
                }
                configured_ = true;
                updateRootLevel(root);
            } catch (SecurityException e) {
                // ignore -- does not have permission to change configuration
            }
            return errors;
        }
    }

    static class LoggingHandler extends Handler {

        @Override
        public void publish(final LogRecord record) {
            if (!isLoggable(record)) {
                return;
            }

            synchronized (lock) {
                if (listener_ == null) {
                    return;
                }

                listener_.onLog(record.getLevel(), record.getMillis(), Thread.currentThread().getName(),
                        Thread.currentThread().threadId(), record.getLoggerName(),
                        getFormatter().formatMessage(record), record.getThrown(),
                        getFormatter().format(record));
            }
        }

        @Override
        public void flush() {

        }

        @Override
        public void close() throws SecurityException {

        }
    }
}
