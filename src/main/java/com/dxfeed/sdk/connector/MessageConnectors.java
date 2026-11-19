// Copyright (c) 2026 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

package com.dxfeed.sdk.connector;

import com.devexperts.qd.qtp.MessageConnector;
import com.devexperts.qd.qtp.QDEndpoint;
import com.devexperts.qd.qtp.socket.ClientSocketConnector;
import com.devexperts.qd.qtp.socket.ServerSocketConnector;
import com.devexperts.qd.stats.QDStats;
import com.devexperts.transport.stats.EndpointStats;
import com.devexperts.util.LogUtil;
import com.dxfeed.api.DXEndpoint;
import com.dxfeed.api.impl.DXEndpointImpl;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * Access to the message connectors of a {@link DXEndpoint}: the connectors QD creates for the address of
 * {@code connect}. Every call reads the current connectors of the endpoint, so the connectors replaced by a
 * {@code connect} to another address are never returned; a replaced connector still answers, as stopped.
 * The values of a connector are read one by one, not as a consistent set.
 */
public final class MessageConnectors {

    /**
     * The kind of a connector: a client socket connector ({@code host:port}).
     */
    public static final int KIND_CLIENT_SOCKET = 0;

    /**
     * The kind of a connector: a server socket connector ({@code :port}).
     */
    public static final int KIND_SERVER_SOCKET = 1;

    /**
     * The kind of a connector: any other connector (a file, a non-blocking or a custom connector).
     */
    public static final int KIND_OTHER = 2;

    private MessageConnectors() {
    }

    /**
     * Returns the QD endpoint behind a {@link DXEndpoint}.
     *
     * @throws IllegalArgumentException if the endpoint is null or not created by {@code DXEndpoint.newBuilder()}
     */
    static QDEndpoint getQDEndpoint(final DXEndpoint endpoint) {
        if (endpoint instanceof DXEndpointImpl impl) {
            return impl.getQDEndpoint();
        }

        throw new IllegalArgumentException(endpoint == null
                ? "The endpoint is null"
                : "Unsupported endpoint implementation: " + endpoint.getClass().getName());
    }

    /**
     * Returns the current connectors of an endpoint: a new list, which later changes of the endpoint do not affect.
     * The list is empty for an endpoint that is not connected or that is closed.
     *
     * @throws IllegalArgumentException if the endpoint is null or not created by {@code DXEndpoint.newBuilder()}
     */
    public static List<MessageConnector> getConnectors(final DXEndpoint endpoint) {
        // QD's own list is live: only read, never changed.
        return new ArrayList<>(getQDEndpoint(endpoint).getConnectors());
    }

    /**
     * @return {@link #KIND_CLIENT_SOCKET}, {@link #KIND_SERVER_SOCKET} or {@link #KIND_OTHER}
     */
    public static int getKind(final MessageConnector connector) {
        Objects.requireNonNull(connector, "connector");

        if (connector instanceof ClientSocketConnector) {
            return KIND_CLIENT_SOCKET;
        }

        if (connector instanceof ServerSocketConnector) {
            return KIND_SERVER_SOCKET;
        }

        return KIND_OTHER;
    }

    /**
     * Returns the address the connector was configured with, user and password masked as in QD's own log.
     */
    public static String getAddress(final MessageConnector connector) {
        Objects.requireNonNull(connector, "connector");

        return LogUtil.hideCredentials(connector.getAddress());
    }

    /**
     * Returns the {@code host:port} addresses a client socket connector is connected to now, one per connected
     * stripe: empty when it is not connected, and always empty for other kinds of connectors.
     */
    public static List<String> getCurrentAddresses(final MessageConnector connector) {
        Objects.requireNonNull(connector, "connector");

        if (connector instanceof ClientSocketConnector client) {
            // A handler that is not connected yet has no address.
            return Arrays.stream(client.getCurrentAddresses()).filter(Objects::nonNull).toList();
        }

        return List.of();
    }

    /**
     * Reads the statistics of a connector. Resets nothing: other readers of the QD statistics see the same values.
     * Without {@code qds-monitoring} (the iOS builds) QD keeps no statistics, and the data record counts are 0.
     */
    public static MessageConnectorStatistics getStatistics(final MessageConnector connector) {
        Objects.requireNonNull(connector, "connector");

        final EndpointStats endpointStats = connector.retrieveCompleteEndpointStats();
        final QDStats stats = connector.getStats();
        final long readDataRecords = stats == null ? 0 : stats.getValue(QDStats.SValue.IO_DATA_READ_RECORDS);
        final long writtenDataRecords = stats == null ? 0 : stats.getValue(QDStats.SValue.IO_DATA_WRITE_RECORDS);

        return new MessageConnectorStatistics(endpointStats.getReadBytes(), endpointStats.getWrittenBytes(),
                readDataRecords, writtenDataRecords, endpointStats.getActiveConnectionCount(),
                endpointStats.getClosedConnectionCount());
    }

    /**
     * Reads the I/O statistics of a connector: the ten cumulative {@code QDStats.SValue.IO_*} counters of
     * {@link MessageConnector#getStats()} and the open and closed connections of
     * {@link MessageConnector#retrieveCompleteEndpointStats()}. Resets nothing: other readers of the QD statistics see
     * the same values. Without {@code qds-monitoring} (the iOS builds) QD keeps no statistics, and the ten counters
     * are 0. They are 0 for a stopped connector too: QD closes its statistics and adds them to those of the endpoint.
     *
     * @since 3.18.0
     */
    public static MessageConnectorIoStatistics getIoStatistics(final MessageConnector connector) {
        Objects.requireNonNull(connector, "connector");

        final EndpointStats endpointStats = connector.retrieveCompleteEndpointStats();
        final QDStats stats = connector.getStats();

        return new MessageConnectorIoStatistics(
                valueOf(stats, QDStats.SValue.IO_DATA_READ_RECORDS),
                valueOf(stats, QDStats.SValue.IO_DATA_WRITE_RECORDS),
                valueOf(stats, QDStats.SValue.IO_SUB_READ_RECORDS),
                valueOf(stats, QDStats.SValue.IO_SUB_WRITE_RECORDS),
                valueOf(stats, QDStats.SValue.IO_READ_BYTES),
                valueOf(stats, QDStats.SValue.IO_WRITE_BYTES),
                valueOf(stats, QDStats.SValue.IO_DATA_READ_LAGS),
                valueOf(stats, QDStats.SValue.IO_DATA_WRITE_LAGS),
                valueOf(stats, QDStats.SValue.IO_READ_RTTS),
                valueOf(stats, QDStats.SValue.IO_WRITE_RTTS),
                endpointStats.getActiveConnectionCount(),
                endpointStats.getClosedConnectionCount());
    }

    private static long valueOf(final QDStats stats, final QDStats.SValue value) {
        return stats == null ? 0 : stats.getValue(value);
    }
}
