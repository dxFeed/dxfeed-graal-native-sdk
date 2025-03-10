// Copyright (c) 2025 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

package com.devexperts.logging;

import java.util.logging.Level;

/**
 * A listener that allows to intercept logged messages.
 */
@FunctionalInterface
public interface InterceptableLoggingListener {

    void onLog(Level level, long timestamp, String threadName, long threadId, String loggerName,
            String message, Throwable exception, String formattedMessage);
}
