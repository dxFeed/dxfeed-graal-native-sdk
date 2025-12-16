// Copyright (c) 2025 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

package com.dxfeed.sdk.events;

import org.graalvm.nativeimage.c.CContext;
import org.graalvm.nativeimage.c.struct.CField;
import org.graalvm.nativeimage.c.struct.CStruct;

@CContext(Directives.class)
@CStruct("dxfg_nuam_trade_t")
public interface DxfgNuamTrade extends DxfgTrade {
    @CField("trade_stat_time")
    long getTradeStatTime();

    @CField("trade_stat_time")
    void setTradeStatTime(long tradeStatTime);

    @CField("last_significant_price")
    double getLastSignificantPrice();

    @CField("last_significant_price")
    void setLastSignificantPrice(double lastSignificantPrice);

    @CField("last_price_for_all")
    double getLastPriceForAll();

    @CField("last_price_for_all")
    void setLastPriceForAll(double lastPriceForAll);

    @CField("number_of_trades")
    int getNumberOfTrades();

    @CField("number_of_trades")
    void setNumberOfTrades(int numberOfTrades);

    @CField("vwap")
    double getVWAP();

    @CField("vwap")
    void setVWAP(double vwap);
}
