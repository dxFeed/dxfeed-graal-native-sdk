// Copyright (c) 2025 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

package com.dxfeed.sdk.events;

import org.graalvm.nativeimage.c.CContext;
import org.graalvm.nativeimage.c.struct.CField;
import org.graalvm.nativeimage.c.struct.CStruct;

@CContext(Directives.class)
@CStruct("dxfg_trade_base_t")
public interface DxfgTradeBase extends DxfgMarketEvent {

    @CField("time_sequence")
    long getTimeSequence();

    @CField("time_sequence")
    void setTimeSequence(long value);

    @CField("time_nano_part")
    int getTimeNanoPart();

    @CField("time_nano_part")
    void setTimeNanoPart(int value);

    @CField("exchange_code")
    char getExchangeCode();

    @CField("exchange_code")
    void setExchangeCode(char value);

    @CField("price")
    double getPrice();

    @CField("price")
    void setPrice(double value);

    @CField("change")
    double getChange();

    @CField("change")
    void setChange(double value);

    @CField("size")
    double getSize();

    @CField("size")
    void setSize(double value);

    @CField("day_id")
    int getDayId();

    @CField("day_id")
    void setDayId(int value);

    @CField("day_volume")
    double getDayVolume();

    @CField("day_volume")
    void setDayVolume(double value);

    @CField("day_turnover")
    double getDayTurnover();

    @CField("day_turnover")
    void setDayTurnover(double value);

    @CField("flags")
    int getFlags();

    @CField("flags")
    void setFlags(int value);

}
