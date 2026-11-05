// Copyright (c) 2026 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

package com.dxfeed.sdk.connector;

import com.devexperts.qd.qtp.MessageConnector;
import com.devexperts.qd.qtp.MessageConnectorListener;
import com.devexperts.qd.qtp.QDEndpoint;
import com.dxfeed.api.DXEndpoint;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * The connector listeners of an endpoint. The monitor is created with the first listener of the endpoint and adds one
 * QD listener to the QD endpoint, which QD also adds to the connectors that a later {@code connect} creates. QD calls
 * it on its socket threads and on the thread of a {@code connect} or a {@code close}, under its locks, so it only
 * passes the connector to {@link ConnectorDispatcher}, which calls the listeners on its own thread.
 */
final class ConnectorMonitor implements MessageConnectorListener {

    // Weak: the monitor does not refer to its endpoint, so an unreachable endpoint drops its monitor.
    private static final Map<DXEndpoint, ConnectorMonitor> MONITORS = new WeakHashMap<>();

    private final CopyOnWriteArrayList<ConnectorStateListener> listeners = new CopyOnWriteArrayList<>();

    private ConnectorMonitor() {
    }

    /**
     * Adds a listener to the endpoint; a listener that is added already stays as it is.
     *
     * @throws IllegalArgumentException if the endpoint is null or not created by {@code DXEndpoint.newBuilder()}
     */
    static void addListener(final DXEndpoint endpoint, final ConnectorStateListener listener) {
        final QDEndpoint qdEndpoint = MessageConnectors.getQDEndpoint(endpoint);
        ConnectorMonitor monitor;

        synchronized (MONITORS) {
            monitor = MONITORS.get(endpoint);

            if (monitor == null) {
                monitor = new ConnectorMonitor();
                // QD calls a new QD listener for the current connectors within the add; the monitor has no listeners
                // yet and drops these calls. Under the lock of the monitors, so a concurrent add waits for it; QD
                // never calls back into the monitors under its own locks, so the order of the locks is fixed.
                qdEndpoint.addMessageConnectionListener(monitor);
                MONITORS.put(endpoint, monitor);
            }
        }

        monitor.listeners.addIfAbsent(listener);
    }

    /**
     * Removes a listener from the endpoint; a listener that is not added is no error.
     *
     * @throws IllegalArgumentException if the endpoint is null or not created by {@code DXEndpoint.newBuilder()}
     */
    static void removeListener(final DXEndpoint endpoint, final ConnectorStateListener listener) {
        // Only the check: a null or foreign endpoint is an error, as for addListener.
        MessageConnectors.getQDEndpoint(endpoint);

        final ConnectorMonitor monitor;

        synchronized (MONITORS) {
            monitor = MONITORS.get(endpoint);
        }

        if (monitor != null) {
            monitor.listeners.remove(listener);
        }
    }

    @Override
    public void stateChanged(final MessageConnector connector) {
        if (!listeners.isEmpty()) {
            ConnectorDispatcher.enqueue(this, connector);
        }
    }

    // On the thread of the dispatcher.
    void dispatch(final MessageConnector connector) {
        for (final ConnectorStateListener listener : listeners) {
            if (!listener.connectorChanged(connector)) {
                listeners.remove(listener);
            }
        }
    }
}
