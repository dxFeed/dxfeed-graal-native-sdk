// Copyright (c) 2026 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

package com.dxfeed.sdk.connector;

import org.graalvm.nativeimage.c.CContext;
import org.graalvm.nativeimage.c.struct.CField;
import org.graalvm.nativeimage.c.struct.CStruct;
import org.graalvm.word.PointerBase;

/**
 * {@code dxfg_v4_message_connector_io_stats_t}: the I/O statistics of a connector, filled in the caller's structure.
 *
 * @since 3.18.0
 */
@CContext(MessageConnectorDirectives.class)
@CStruct("dxfg_v4_message_connector_io_stats_t")
public interface DxfgMessageConnectorIoStats extends PointerBase {

    @CField("read_data_records")
    void setReadDataRecords(long value);

    @CField("written_data_records")
    void setWrittenDataRecords(long value);

    @CField("read_sub_records")
    void setReadSubRecords(long value);

    @CField("written_sub_records")
    void setWrittenSubRecords(long value);

    @CField("read_bytes")
    void setReadBytes(long value);

    @CField("written_bytes")
    void setWrittenBytes(long value);

    @CField("read_data_lag_sum_us")
    void setReadDataLagSumMicros(long value);

    @CField("written_data_lag_sum_us")
    void setWrittenDataLagSumMicros(long value);

    @CField("read_rtt_sum_us")
    void setReadRttSumMicros(long value);

    @CField("written_rtt_sum_us")
    void setWrittenRttSumMicros(long value);

    @CField("active_connection_count")
    void setActiveConnectionCount(long value);

    @CField("closed_connection_count")
    void setClosedConnectionCount(long value);
}
