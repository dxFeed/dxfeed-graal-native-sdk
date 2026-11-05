// Copyright (c) 2026 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

package com.dxfeed.sdk.connector;

import com.devexperts.qd.qtp.MessageConnector;
import com.dxfeed.api.DXEndpoint;
import com.dxfeed.api.impl.DXEndpointImpl;
import java.util.Map;
import java.util.Objects;
import java.util.WeakHashMap;

/**
 * The last connection errors of the message connectors and the endpoints, captured from QD's log records (see
 * {@link ConnectionErrorCapture}) and attributed by the connector monitor of each endpoint.
 */
public final class ConnectionErrors {

    // A captured record older than this when its thread notifies the listeners is not the cause of the change.
    static final long MAX_RECORD_AGE_MILLIS = 10_000;

    // Guarded by itself. Weak: a connector that became unreachable has no errors to read.
    private static final Map<MessageConnector, ConnectorErrors> CONNECTORS = new WeakHashMap<>();

    private ConnectionErrors() {
    }

    /**
     * Starts to capture the connection errors of an endpoint created through the SDK; the first call also adds the
     * capture handler to QD's loggers. Endpoints of other implementations are ignored.
     */
    public static void watch(final DXEndpoint endpoint) {
        if (endpoint instanceof DXEndpointImpl) {
            ConnectionErrorCapture.install();
            ConnectorMonitor.attach(endpoint);
        }
    }

    /**
     * Sets the levels of QD's loggers that the capture needs anew; called after the user's logging levels changed.
     */
    public static void loggingLevelsChanged() {
        ConnectionErrorCapture.updateLevels();
    }

    /**
     * @return the last error of the connector, or {@code null} when it had none
     */
    public static ConnectionError getLastError(final MessageConnector connector) {
        Objects.requireNonNull(connector, "connector");

        synchronized (CONNECTORS) {
            final ConnectorErrors errors = CONNECTORS.get(connector);

            return errors == null ? null : errors.last;
        }
    }

    /**
     * @return the newest error of the connectors of the endpoint, also of the connectors that a connect to another
     * address replaced, or {@code null} when there was none
     * @throws IllegalArgumentException if the endpoint is null or not created by {@code DXEndpoint.newBuilder()}
     */
    public static ConnectionError getLastError(final DXEndpoint endpoint) {
        return ConnectorMonitor.getLastError(endpoint);
    }

    /**
     * Attributes a captured record to the connector whose listeners its thread notifies.
     *
     * @return the error, or {@code null} if the record is not of this connector or too old
     */
    static ConnectionError record(final MessageConnector connector, final ConnectionErrorCapture.Pending pending) {
        if (!isOfConnector(pending.loggerName(), connector)
                || System.currentTimeMillis() - pending.time() > MAX_RECORD_AGE_MILLIS) {
            return null;
        }

        final long closedConnectionCount = connector.getClosedConnectionCount();
        final String address = MessageConnectors.getAddress(connector);

        synchronized (CONNECTORS) {
            final ConnectorErrors errors = CONNECTORS.computeIfAbsent(connector, key -> new ConnectorErrors());
            final boolean connectPhase = pending.category() == ConnectionError.CATEGORY_DNS
                    || pending.category() == ConnectionError.CATEGORY_CONNECT_FAILED;
            // The closed connections grow when an established connection ends, not when a connect fails. Compared
            // with the previous error, not the previous change: the other worker thread of the connection may
            // notify the listeners before this one.
            final boolean dropped = !connectPhase && closedConnectionCount > errors.closedConnectionCount;
            // A read timeout before any connection is a timeout of the connect (a proxy's reply).
            final int category = pending.category() == ConnectionError.CATEGORY_READ_TIMEOUT && !dropped
                    ? ConnectionError.CATEGORY_CONNECT_FAILED
                    : pending.category();

            errors.closedConnectionCount = closedConnectionCount;
            errors.count++;
            errors.last = new ConnectionError(pending.time(), address, category, dropped,
                    pending.exceptionClassName(), pending.message(), errors.count);

            return errors.last;
        }
    }

    private static boolean isOfConnector(final String loggerName, final MessageConnector connector) {
        if (loggerName == null) {
            return false;
        }

        return loggerName.equals(ConnectionErrorCapture.CONNECTOR_LOGGER + "." + connector.getName())
                || loggerName.equals(ConnectionErrorCapture.HEARTBEAT_LOGGER)
                || loggerName.startsWith(ConnectionErrorCapture.TLS_LOGGER + ".");
    }

    // Guarded by CONNECTORS.
    private static final class ConnectorErrors {
        long closedConnectionCount;
        long count;
        ConnectionError last;
    }
}
