// Copyright (c) 2026 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

package com.dxfeed.sdk.connector;

/**
 * The I/O statistics of a message connector at one moment: the ten cumulative {@code QDStats.SValue.IO_*} counters of
 * QD and the open and closed connections. The counters cover all the connections of the connector since it was
 * created, the closed ones included; reading them resets nothing that other readers see. Without
 * {@code qds-monitoring} (the iOS builds) QD keeps no statistics, and the ten counters are 0.
 *
 * <p>The lags and the round-trip times are sums in microseconds over the records: QD adds the current value of the
 * connection for each record, so a sum divided by the matching record count is an average per record.
 *
 * @since 3.18.0
 */
public final class MessageConnectorIoStatistics {

    private final long readDataRecords;
    private final long writtenDataRecords;
    private final long readSubRecords;
    private final long writtenSubRecords;
    private final long readBytes;
    private final long writtenBytes;
    private final long readDataLagSumMicros;
    private final long writtenDataLagSumMicros;
    private final long readRttSumMicros;
    private final long writtenRttSumMicros;
    private final long activeConnectionCount;
    private final long closedConnectionCount;

    MessageConnectorIoStatistics(final long readDataRecords, final long writtenDataRecords, final long readSubRecords,
            final long writtenSubRecords, final long readBytes, final long writtenBytes,
            final long readDataLagSumMicros, final long writtenDataLagSumMicros, final long readRttSumMicros,
            final long writtenRttSumMicros, final long activeConnectionCount, final long closedConnectionCount) {
        this.readDataRecords = readDataRecords;
        this.writtenDataRecords = writtenDataRecords;
        this.readSubRecords = readSubRecords;
        this.writtenSubRecords = writtenSubRecords;
        this.readBytes = readBytes;
        this.writtenBytes = writtenBytes;
        this.readDataLagSumMicros = readDataLagSumMicros;
        this.writtenDataLagSumMicros = writtenDataLagSumMicros;
        this.readRttSumMicros = readRttSumMicros;
        this.writtenRttSumMicros = writtenRttSumMicros;
        this.activeConnectionCount = activeConnectionCount;
        this.closedConnectionCount = closedConnectionCount;
    }

    /**
     * @return the data records read ({@code IO_DATA_READ_RECORDS})
     */
    public long getReadDataRecords() {
        return readDataRecords;
    }

    /**
     * @return the data records written ({@code IO_DATA_WRITE_RECORDS})
     */
    public long getWrittenDataRecords() {
        return writtenDataRecords;
    }

    /**
     * @return the subscription records read ({@code IO_SUB_READ_RECORDS}): those a publisher receives
     */
    public long getReadSubRecords() {
        return readSubRecords;
    }

    /**
     * @return the subscription records written ({@code IO_SUB_WRITE_RECORDS}): those a feed sends
     */
    public long getWrittenSubRecords() {
        return writtenSubRecords;
    }

    /**
     * @return the bytes of the QTP messages read, heartbeats included ({@code IO_READ_BYTES})
     */
    public long getReadBytes() {
        return readBytes;
    }

    /**
     * @return the bytes of the QTP messages written, heartbeats included ({@code IO_WRITE_BYTES})
     */
    public long getWrittenBytes() {
        return writtenBytes;
    }

    /**
     * @return the sum over the data records read of their lag in microseconds ({@code IO_DATA_READ_LAGS})
     */
    public long getReadDataLagSumMicros() {
        return readDataLagSumMicros;
    }

    /**
     * @return the sum over the data records written of their lag in microseconds ({@code IO_DATA_WRITE_LAGS})
     */
    public long getWrittenDataLagSumMicros() {
        return writtenDataLagSumMicros;
    }

    /**
     * @return the sum over the records read of the round-trip time of the connection in microseconds
     *     ({@code IO_READ_RTTS})
     */
    public long getReadRttSumMicros() {
        return readRttSumMicros;
    }

    /**
     * @return the sum over the records written of the round-trip time of the connection in microseconds
     *     ({@code IO_WRITE_RTTS})
     */
    public long getWrittenRttSumMicros() {
        return writtenRttSumMicros;
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
        return "MessageConnectorIoStatistics{readDataRecords=" + readDataRecords +
                ", writtenDataRecords=" + writtenDataRecords + ", readSubRecords=" + readSubRecords +
                ", writtenSubRecords=" + writtenSubRecords + ", readBytes=" + readBytes +
                ", writtenBytes=" + writtenBytes + ", readDataLagSumMicros=" + readDataLagSumMicros +
                ", writtenDataLagSumMicros=" + writtenDataLagSumMicros + ", readRttSumMicros=" + readRttSumMicros +
                ", writtenRttSumMicros=" + writtenRttSumMicros + ", activeConnectionCount=" + activeConnectionCount +
                ", closedConnectionCount=" + closedConnectionCount + "}";
    }
}
