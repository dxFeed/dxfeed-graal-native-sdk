// Copyright (c) 2025 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

package com.dxfeed.sdk.events;

import org.graalvm.nativeimage.c.CContext;
import org.graalvm.nativeimage.c.struct.CField;
import org.graalvm.nativeimage.c.struct.CStruct;

@CContext(Directives.class)
@CStruct("dxfg_otc_markets_order_t")
public interface DxfgOtcMarketsOrder extends DxfgOrder {

    @CField("quote_access_payment")
    int getQuoteAccessPayment();

    @CField("quote_access_payment")
    void setQuoteAccessPayment(int value);

    @CField("otc_markets_flags")
    int getOtcMarketsFlags();

    @CField("otc_markets_flags")
    void setOtcMarketsFlags(int value);
}
