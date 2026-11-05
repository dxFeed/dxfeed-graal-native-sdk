// Copyright (c) 2026 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

package com.dxfeed.sdk.connector;

import com.devexperts.logging.Logging;
import com.devexperts.qd.qtp.MessageConnector;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * The thread of the isolate that calls the connector listeners: one daemon thread, started with the first state change
 * to deliver. The changes are delivered in their order; a change of a connector that still waits is not queued again,
 * since the listener reads the current state. {@link #shutdown()} stops the thread for good.
 */
public final class ConnectorDispatcher {

    static final String THREAD_NAME = "dxfg-MessageConnectorListener";

    private static final Logging log = Logging.getLogging(ConnectorDispatcher.class);

    private static final Object LOCK = new Object();
    // Guarded by LOCK: the connectors whose change waits, in their order, with their monitors.
    private static final Map<MessageConnector, ConnectorMonitor> PENDING = new LinkedHashMap<>();
    private static Thread thread;
    private static boolean stopped;

    private ConnectorDispatcher() {
    }

    static void enqueue(final ConnectorMonitor monitor, final MessageConnector connector) {
        synchronized (LOCK) {
            if (stopped) {
                return;
            }

            PENDING.putIfAbsent(connector, monitor);

            if (thread == null) {
                thread = new Thread(ConnectorDispatcher::run, THREAD_NAME);
                thread.setDaemon(true);
                thread.start();
            }

            LOCK.notifyAll();
        }
    }

    /**
     * Stops the thread and waits until it ends; the changes that wait are dropped, and later ones are not delivered.
     * Called by {@code dxfg_v4_system_shutdown} after the listeners are deactivated, so no listener call is running.
     */
    public static void shutdown() throws InterruptedException {
        final Thread current;

        synchronized (LOCK) {
            stopped = true;
            PENDING.clear();
            current = thread;
            LOCK.notifyAll();
        }

        if (current != null && current != Thread.currentThread()) {
            current.join();
        }
    }

    private static void run() {
        while (true) {
            final MessageConnector connector;
            final ConnectorMonitor monitor;

            synchronized (LOCK) {
                while (PENDING.isEmpty() && !stopped) {
                    try {
                        LOCK.wait();
                    } catch (final InterruptedException e) {
                        // Only shutdown() ends the thread.
                    }
                }

                if (stopped) {
                    return;
                }

                final Iterator<Map.Entry<MessageConnector, ConnectorMonitor>> first = PENDING.entrySet().iterator();
                final Map.Entry<MessageConnector, ConnectorMonitor> entry = first.next();
                connector = entry.getKey();
                monitor = entry.getValue();
                first.remove();
            }

            try {
                monitor.dispatch(connector);
            } catch (final RuntimeException | Error e) {
                log.error("A connector listener failed", e);
            }
        }
    }
}
