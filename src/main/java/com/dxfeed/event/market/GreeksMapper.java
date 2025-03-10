// Copyright (c) 2025 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

package com.dxfeed.event.market;

import com.dxfeed.event.option.Greeks;
import com.dxfeed.sdk.events.DxfgEventClazz;
import com.dxfeed.sdk.events.DxfgGreeks;
import com.dxfeed.sdk.mappers.Mapper;
import org.graalvm.nativeimage.UnmanagedMemory;
import org.graalvm.nativeimage.c.struct.SizeOf;
import org.graalvm.nativeimage.c.type.CCharPointer;

public class GreeksMapper extends MarketEventMapper<Greeks, DxfgGreeks> {

    public GreeksMapper(
            final Mapper<String, CCharPointer> stringMapperForMarketEvent
    ) {
        super(stringMapperForMarketEvent);
    }

    @Override
    public DxfgGreeks createNativeObject() {
        final DxfgGreeks nativeObject = UnmanagedMemory.calloc(SizeOf.get(DxfgGreeks.class));
        nativeObject.setClazz(DxfgEventClazz.DXFG_EVENT_GREEKS.getCValue());
        return nativeObject;
    }

    @Override
    public void fillNative(final Greeks javaObject, final DxfgGreeks nativeObject, boolean clean) {
        super.fillNative(javaObject, nativeObject, clean);
        nativeObject.setEventFlags(javaObject.getEventFlags());
        nativeObject.setIndex(javaObject.getIndex());
        nativeObject.setPrice(javaObject.getPrice());
        nativeObject.setVolatility(javaObject.getVolatility());
        nativeObject.setDelta(javaObject.getDelta());
        nativeObject.setGamma(javaObject.getGamma());
        nativeObject.setTheta(javaObject.getTheta());
        nativeObject.setRho(javaObject.getRho());
        nativeObject.setVega(javaObject.getVega());
    }

    @Override
    public void cleanNative(final DxfgGreeks nativeObject) {
        super.cleanNative(nativeObject);
    }

    @Override
    protected Greeks doToJava(final DxfgGreeks nativeObject) {
        final Greeks javaObject = new Greeks();
        fillJava(nativeObject, javaObject);
        return javaObject;
    }

    @Override
    public void fillJava(final DxfgGreeks nativeObject, final Greeks javaObject) {
        super.fillJava(nativeObject, javaObject);
        javaObject.setEventFlags(nativeObject.getEventFlags());
        javaObject.setIndex(nativeObject.getIndex());
        javaObject.setPrice(nativeObject.getPrice());
        javaObject.setVolatility(nativeObject.getVolatility());
        javaObject.setDelta(nativeObject.getDelta());
        javaObject.setGamma(nativeObject.getGamma());
        javaObject.setTheta(nativeObject.getTheta());
        javaObject.setRho(nativeObject.getRho());
        javaObject.setVega(nativeObject.getVega());
    }
}
