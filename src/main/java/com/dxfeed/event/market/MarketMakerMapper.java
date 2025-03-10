// Copyright (c) 2025 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

package com.dxfeed.event.market;

import com.dxfeed.sdk.events.DxfgEventClazz;
import com.dxfeed.sdk.events.DxfgMarketMaker;
import com.dxfeed.sdk.mappers.Mapper;
import org.graalvm.nativeimage.UnmanagedMemory;
import org.graalvm.nativeimage.c.struct.SizeOf;
import org.graalvm.nativeimage.c.type.CCharPointer;

public class MarketMakerMapper
        extends MarketEventMapper<MarketMaker, DxfgMarketMaker> {

    public MarketMakerMapper(
            final Mapper<String, CCharPointer> stringMapperForMarketEvent
    ) {
        super(stringMapperForMarketEvent);
    }

    @Override
    public DxfgMarketMaker createNativeObject() {
        final DxfgMarketMaker nativeObject = UnmanagedMemory.calloc(SizeOf.get(DxfgMarketMaker.class));

        nativeObject.setClazz(DxfgEventClazz.DXFG_EVENT_MARKET_MAKER.getCValue());

        return nativeObject;
    }

    @Override
    public void fillNative(final MarketMaker javaObject, final DxfgMarketMaker nativeObject,
            boolean clean) {
        super.fillNative(javaObject, nativeObject, clean);
        nativeObject.setEventFlags(javaObject.getEventFlags());
        nativeObject.setIndex(javaObject.getIndex());
        nativeObject.setBidTime(javaObject.getBidTime());
        nativeObject.setBidPrice(javaObject.getBidPrice());
        nativeObject.setBidSize(javaObject.getBidSize());
        nativeObject.setBidCount(javaObject.getBidCount());
        nativeObject.setAskTime(javaObject.getAskTime());
        nativeObject.setAskPrice(javaObject.getAskPrice());
        nativeObject.setAskSize(javaObject.getAskSize());
        nativeObject.setAskCount(javaObject.getAskCount());
    }

    @Override
    public void cleanNative(final DxfgMarketMaker nativeObject) {
        super.cleanNative(nativeObject);
    }

    @Override
    protected MarketMaker doToJava(final DxfgMarketMaker nativeObject) {
        final MarketMaker javaObject = new MarketMaker();

        this.fillJava(nativeObject, javaObject);

        return javaObject;
    }

    @Override
    public void fillJava(final DxfgMarketMaker nativeObject, final MarketMaker javaObject) {
        super.fillJava(nativeObject, javaObject);
        javaObject.setEventFlags(nativeObject.getEventFlags());
        javaObject.setIndex(nativeObject.getIndex());
        javaObject.setBidTime(nativeObject.getBidTime());
        javaObject.setBidPrice(nativeObject.getBidPrice());
        javaObject.setBidSize(nativeObject.getBidSize());
        javaObject.setBidCount(nativeObject.getBidCount());
        javaObject.setAskTime(nativeObject.getAskTime());
        javaObject.setAskPrice(nativeObject.getAskPrice());
        javaObject.setAskSize(nativeObject.getAskSize());
        javaObject.setAskCount(nativeObject.getAskCount());
    }
}

