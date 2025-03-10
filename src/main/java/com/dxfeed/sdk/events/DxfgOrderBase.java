// Copyright (c) 2025 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

package com.dxfeed.sdk.events;

import org.graalvm.nativeimage.c.CContext;
import org.graalvm.nativeimage.c.struct.CField;
import org.graalvm.nativeimage.c.struct.CStruct;

@CContext(Directives.class)
@CStruct("dxfg_order_base_t")
public interface DxfgOrderBase extends DxfgMarketEvent {

    @CField("event_flags")
    int getEventFlags();

    @CField("event_flags")
    void setEventFlags(int value);

    @CField("index")
    long getIndex();

    @CField("index")
    void setIndex(long value);

    @CField("time_sequence")
    long getTimeSequence();

    @CField("time_sequence")
    void setTimeSequence(long value);

    @CField("time_nano_part")
    int getTimeNanoPart();

    @CField("time_nano_part")
    void setTimeNanoPart(int value);

    @CField("action_time")
    long getActionTime();

    @CField("action_time")
    void setActionTime(long value);

    @CField("order_id")
    long getOrderId();

    @CField("order_id")
    void setOrderId(long value);

    @CField("aux_order_id")
    long getAuxOrderId();

    @CField("aux_order_id")
    void setAuxOrderId(long value);

    @CField("price")
    double getPrice();

    @CField("price")
    void setPrice(double value);

    @CField("size")
    double getSize();

    @CField("size")
    void setSize(double value);

    @CField("executed_size")
    double getExecutedSize();

    @CField("executed_size")
    void setExecutedSize(double value);

    @CField("count")
    long getCount();

    @CField("count")
    void setCount(long value);

    @CField("flags")
    int getFlags();

    @CField("flags")
    void setFlags(int value);

    @CField("trade_id")
    long getTradeId();

    @CField("trade_id")
    void setTradeId(long value);

    @CField("trade_price")
    double getTradePrice();

    @CField("trade_price")
    void setTradePrice(double value);

    @CField("trade_size")
    double getTradeSize();

    @CField("trade_size")
    void setTradeSize(double value);
}
