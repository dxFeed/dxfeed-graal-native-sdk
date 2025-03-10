// Copyright (c) 2025 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

package com.dxfeed.sdk.events;

import org.graalvm.nativeimage.c.CContext;
import org.graalvm.nativeimage.c.struct.CField;
import org.graalvm.nativeimage.c.struct.CStruct;

@CContext(Directives.class)
@CStruct("dxfg_market_maker_t")
public interface DxfgMarketMaker extends DxfgMarketEvent {

    @CField("event_flags")
    int getEventFlags();

    @CField("event_flags")
    void setEventFlags(int value);

    @CField("index")
    long getIndex();

    @CField("index")
    void setIndex(long value);

    @CField("bid_time")
    long getBidTime();

    @CField("bid_time")
    void setBidTime(long bidTime);

    @CField("bid_price")
    double getBidPrice();

    @CField("bid_price")
    void setBidPrice(double bidPrice);

    @CField("bid_size")
    double getBidSize();

    @CField("bid_size")
    void setBidSize(double bidSize);

    @CField("bid_count")
    long getBidCount();

    @CField("bid_count")
    void setBidCount(long bidCount);

    @CField("ask_time")
    long getAskTime();

    @CField("ask_time")
    void setAskTime(long askTime);

    @CField("ask_price")
    double getAskPrice();

    @CField("ask_price")
    void setAskPrice(double askPrice);

    @CField("ask_size")
    double getAskSize();

    @CField("ask_size")
    void setAskSize(double askSize);

    @CField("ask_count")
    long getAskCount();

    @CField("ask_count")
    void setAskCount(long askCount);
}