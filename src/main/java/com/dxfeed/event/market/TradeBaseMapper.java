// Copyright (c) 2025 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

package com.dxfeed.event.market;

import com.dxfeed.sdk.events.DxfgTradeBase;
import com.dxfeed.sdk.mappers.Mapper;
import org.graalvm.nativeimage.c.type.CCharPointer;

public abstract class TradeBaseMapper<V extends TradeBase, T extends DxfgTradeBase>
        extends MarketEventMapper<V, T> {

    public TradeBaseMapper(
            final Mapper<String, CCharPointer> stringMapperForMarketEvent
    ) {
        super(stringMapperForMarketEvent);
    }

    @Override
    public void fillNative(final V javaObject, final T nativeObject, boolean clean) {
        super.fillNative(javaObject, nativeObject, clean);
        nativeObject.setTimeSequence(javaObject.getTimeSequence());
        nativeObject.setTimeNanoPart(javaObject.getTimeNanoPart());
        nativeObject.setExchangeCode(javaObject.getExchangeCode());
        nativeObject.setPrice(javaObject.getPrice());
        nativeObject.setChange(javaObject.getChange());
        nativeObject.setSize(javaObject.getSizeAsDouble());
        nativeObject.setDayId(javaObject.getDayId());
        nativeObject.setDayVolume(javaObject.getDayVolumeAsDouble());
        nativeObject.setDayTurnover(javaObject.getDayTurnover());
        nativeObject.setFlags(javaObject.getFlags());
        nativeObject.setTradeId(javaObject.getTradeId());
    }

    @Override
    public void cleanNative(final T nativeObject) {
        super.cleanNative(nativeObject);
    }

    @Override
    public void fillJava(final T nativeObject, final V javaObject) {
        super.fillJava(nativeObject, javaObject);
        javaObject.setTimeSequence(nativeObject.getTimeSequence());
        javaObject.setTimeNanoPart(nativeObject.getTimeNanoPart());
        javaObject.setExchangeCode(nativeObject.getExchangeCode());
        javaObject.setPrice(nativeObject.getPrice());
        javaObject.setChange(nativeObject.getChange());
        javaObject.setSizeAsDouble(nativeObject.getSize());
        javaObject.setDayId(nativeObject.getDayId());
        javaObject.setDayVolumeAsDouble(nativeObject.getDayVolume());
        javaObject.setDayTurnover(nativeObject.getDayTurnover());
        javaObject.setFlags(nativeObject.getFlags());
        javaObject.setTradeId(nativeObject.getTradeId());
    }
}
