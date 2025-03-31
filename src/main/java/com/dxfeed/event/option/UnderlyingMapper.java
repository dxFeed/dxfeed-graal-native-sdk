// Copyright (c) 2025 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

package com.dxfeed.event.option;

import com.dxfeed.event.market.MarketEventMapper;
import com.dxfeed.sdk.events.DxfgEventClazz;
import com.dxfeed.sdk.events.DxfgUnderlying;
import com.dxfeed.sdk.mappers.Mapper;
import org.graalvm.nativeimage.UnmanagedMemory;
import org.graalvm.nativeimage.c.struct.SizeOf;
import org.graalvm.nativeimage.c.type.CCharPointer;

public class UnderlyingMapper extends MarketEventMapper<Underlying, DxfgUnderlying> {

    public UnderlyingMapper(
            final Mapper<String, CCharPointer> stringMapperForMarketEvent
    ) {
        super(stringMapperForMarketEvent);
    }

    @Override
    public DxfgUnderlying createNativeObject() {
        final DxfgUnderlying nativeObject = UnmanagedMemory.calloc(SizeOf.get(DxfgUnderlying.class));
        nativeObject.setClazz(DxfgEventClazz.DXFG_EVENT_UNDERLYING.getCValue());
        return nativeObject;
    }

    @Override
    public DxfgEventClazz getEventClazz() {
        return DxfgEventClazz.DXFG_EVENT_UNDERLYING;
    }

    @Override
    public void fillNative(final Underlying javaObject, final DxfgUnderlying nativeObject, boolean clean) {
        super.fillNative(javaObject, nativeObject, clean);
        nativeObject.setEventFlags(javaObject.getEventFlags());
        nativeObject.setIndex(javaObject.getIndex());
        nativeObject.setVolatility(javaObject.getVolatility());
        nativeObject.setFrontVolatility(javaObject.getFrontVolatility());
        nativeObject.setBackVolatility(javaObject.getBackVolatility());
        nativeObject.setCallVolume(javaObject.getCallVolume());
        nativeObject.setPutVolume(javaObject.getPutVolume());
        nativeObject.setPutCallRatio(javaObject.getPutCallRatio());
    }

    @Override
    public void cleanNative(final DxfgUnderlying nativeObject) {
        super.cleanNative(nativeObject);
    }

    @Override
    protected Underlying doToJava(final DxfgUnderlying nativeObject) {
        final Underlying javaObject = new Underlying();
        this.fillJava(nativeObject, javaObject);
        return javaObject;
    }

    @Override
    public void fillJava(final DxfgUnderlying nativeObject, final Underlying javaObject) {
        super.fillJava(nativeObject, javaObject);
        javaObject.setEventFlags(nativeObject.getEventFlags());
        javaObject.setIndex(nativeObject.getIndex());
        javaObject.setVolatility(nativeObject.getVolatility());
        javaObject.setFrontVolatility(nativeObject.getFrontVolatility());
        javaObject.setBackVolatility(nativeObject.getBackVolatility());
        javaObject.setCallVolume(nativeObject.getCallVolume());
        javaObject.setPutVolume(nativeObject.getPutVolume());
        javaObject.setPutCallRatio(nativeObject.getPutCallRatio());
    }
}
