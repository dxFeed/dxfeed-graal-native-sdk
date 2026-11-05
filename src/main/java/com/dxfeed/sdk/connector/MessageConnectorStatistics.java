// Copyright (c) 2026 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

package com.dxfeed.sdk.connector;

/**
 * Statistics of a message connector at one moment. The counters grow over the life of the connector: those of closed
 * connections are kept. Reading them resets nothing that other readers see.
 */
public final class MessageConnectorStatistics {

    private final long readBytes;
    private final long writtenBytes;
    private final long readDataRecords;
    private final long writtenDataRecords;
    private final long activeConnectionCount;
    private final long closedConnectionCount;

    MessageConnectorStatistics(final long readBytes, final long writtenBytes, final long readDataRecords,
            final long writtenDataRecords, final long activeConnectionCount, final long closedConnectionCount) {
        this.readBytes = readBytes;
        this.writtenBytes = writtenBytes;
        this.readDataRecords = readDataRecords;
        this.writtenDataRecords = writtenDataRecords;
        this.activeConnectionCount = activeConnectionCount;
        this.closedConnectionCount = closedConnectionCount;
    }

    /**
     * @return the bytes read by all connections of the connector, heartbeats included
     */
    public long getReadBytes() {
        return readBytes;
    }

    /**
     * @return the bytes written by all connections of the connector, heartbeats included
     */
    public long getWrittenBytes() {
        return writtenBytes;
    }

    /**
     * @return the data records read by all connections of the connector: grows only when data arrives
     */
    public long getReadDataRecords() {
        return readDataRecords;
    }

    /**
     * @return the data records written by all connections of the connector
     */
    public long getWrittenDataRecords() {
        return writtenDataRecords;
    }

    /**
     * @return the open connections of the connector
     */
    public long getActiveConnectionCount() {
        return activeConnectionCount;
    }

    /**
     * @return the connections of the connector closed since it was created
     */
    public long getClosedConnectionCount() {
        return closedConnectionCount;
    }

    @Override
    public String toString() {
        return "MessageConnectorStatistics{readBytes=" + readBytes + ", writtenBytes=" + writtenBytes +
                ", readDataRecords=" + readDataRecords + ", writtenDataRecords=" + writtenDataRecords +
                ", activeConnectionCount=" + activeConnectionCount +
                ", closedConnectionCount=" + closedConnectionCount + "}";
    }
}
