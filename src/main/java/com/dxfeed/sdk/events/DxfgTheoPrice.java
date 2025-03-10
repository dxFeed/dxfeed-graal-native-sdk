// Copyright (c) 2025 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

package com.dxfeed.sdk.events;

import org.graalvm.nativeimage.c.CContext;
import org.graalvm.nativeimage.c.struct.CField;
import org.graalvm.nativeimage.c.struct.CStruct;

@CContext(Directives.class)
@CStruct("dxfg_theo_price_t")
public interface DxfgTheoPrice extends DxfgMarketEvent {

    @CField("event_flags")
    int getEventFlags();

    @CField("event_flags")
    void setEventFlags(int value);

    @CField("index")
    long getIndex();

    @CField("index")
    void setIndex(long value);

    @CField("price")
    double getPrice();

    @CField("price")
    void setPrice(double value);

    @CField("underlying_price")
    double getUnderlyingPrice();

    @CField("underlying_price")
    void setUnderlyingPrice(double value);

    @CField("delta")
    double getDelta();

    @CField("delta")
    void setDelta(double value);

    @CField("gamma")
    double getGamma();

    @CField("gamma")
    void setGamma(double value);

    @CField("dividend")
    double getDividend();

    @CField("dividend")
    void setDividend(double value);

    @CField("interest")
    double getInterest();

    @CField("interest")
    void setInterest(double value);
}
