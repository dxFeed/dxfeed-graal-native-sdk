// Copyright (c) 2026 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

package com.dxfeed.sdk.connector;

import org.graalvm.nativeimage.c.CContext;
import org.graalvm.nativeimage.c.struct.CField;
import org.graalvm.nativeimage.c.struct.CStruct;
import org.graalvm.word.PointerBase;

/**
 * {@code dxfg_v4_message_connector_stats_t}: the statistics of a connector, filled in the caller's structure.
 */
@CContext(MessageConnectorDirectives.class)
@CStruct("dxfg_v4_message_connector_stats_t")
public interface DxfgMessageConnectorStats extends PointerBase {

    @CField("read_bytes")
    void setReadBytes(long value);

    @CField("written_bytes")
    void setWrittenBytes(long value);

    @CField("read_data_records")
    void setReadDataRecords(long value);

    @CField("written_data_records")
    void setWrittenDataRecords(long value);

    @CField("active_connection_count")
    void setActiveConnectionCount(long value);

    @CField("closed_connection_count")
    void setClosedConnectionCount(long value);
}
