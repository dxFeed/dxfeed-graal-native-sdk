// Copyright (c) 2025 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

package com.dxfeed.event.option;

import com.dxfeed.event.market.MarketEventMapper;
import com.dxfeed.sdk.events.DxfgEventClazz;
import com.dxfeed.sdk.events.DxfgTheoPrice;
import com.dxfeed.sdk.mappers.Mapper;
import org.graalvm.nativeimage.UnmanagedMemory;
import org.graalvm.nativeimage.c.struct.SizeOf;
import org.graalvm.nativeimage.c.type.CCharPointer;

public class TheoPriceMapper extends MarketEventMapper<TheoPrice, DxfgTheoPrice> {

    public TheoPriceMapper(
            final Mapper<String, CCharPointer> stringMapperForMarketEvent
    ) {
        super(stringMapperForMarketEvent);
    }

    @Override
    public DxfgTheoPrice createNativeObject() {
        final DxfgTheoPrice nativeObject = UnmanagedMemory.calloc(SizeOf.get(DxfgTheoPrice.class));
        nativeObject.setClazz(DxfgEventClazz.DXFG_EVENT_THEO_PRICE.getCValue());
        return nativeObject;
    }

    @Override
    public DxfgEventClazz getEventClazz() {
        return DxfgEventClazz.DXFG_EVENT_THEO_PRICE;
    }

    @Override
    public void fillNative(final TheoPrice javaObject, final DxfgTheoPrice nativeObject, boolean clean) {
        super.fillNative(javaObject, nativeObject, clean);
        nativeObject.setEventFlags(javaObject.getEventFlags());
        nativeObject.setIndex(javaObject.getIndex());
        nativeObject.setPrice(javaObject.getPrice());
        nativeObject.setUnderlyingPrice(javaObject.getUnderlyingPrice());
        nativeObject.setDelta(javaObject.getDelta());
        nativeObject.setGamma(javaObject.getGamma());
        nativeObject.setDividend(javaObject.getDividend());
        nativeObject.setInterest(javaObject.getInterest());
    }

    @Override
    public void cleanNative(final DxfgTheoPrice nativeObject) {
        super.cleanNative(nativeObject);
    }

    @Override
    protected TheoPrice doToJava(final DxfgTheoPrice nativeObject) {
        final TheoPrice javaObject = new TheoPrice();
        this.fillJava(nativeObject, javaObject);
        return javaObject;
    }

    @Override
    public void fillJava(final DxfgTheoPrice nativeObject, final TheoPrice javaObject) {
        super.fillJava(nativeObject, javaObject);
        javaObject.setEventFlags(nativeObject.getEventFlags());
        javaObject.setIndex(nativeObject.getIndex());
        javaObject.setPrice(nativeObject.getPrice());
        javaObject.setUnderlyingPrice(nativeObject.getUnderlyingPrice());
        javaObject.setDelta(nativeObject.getDelta());
        javaObject.setGamma(nativeObject.getGamma());
        javaObject.setDividend(nativeObject.getDividend());
        javaObject.setInterest(nativeObject.getInterest());
    }
}
