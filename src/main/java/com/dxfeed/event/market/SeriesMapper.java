// Copyright (c) 2025 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

package com.dxfeed.event.market;

import com.dxfeed.event.option.Series;
import com.dxfeed.sdk.events.DxfgEventClazz;
import com.dxfeed.sdk.events.DxfgSeries;
import com.dxfeed.sdk.mappers.Mapper;
import org.graalvm.nativeimage.UnmanagedMemory;
import org.graalvm.nativeimage.c.struct.SizeOf;
import org.graalvm.nativeimage.c.type.CCharPointer;

public class SeriesMapper extends MarketEventMapper<Series, DxfgSeries> {

    public SeriesMapper(
            final Mapper<String, CCharPointer> stringMapperForMarketEvent
    ) {
        super(stringMapperForMarketEvent);
    }

    @Override
    public DxfgSeries createNativeObject() {
        final DxfgSeries nativeObject = UnmanagedMemory.calloc(SizeOf.get(DxfgSeries.class));
        nativeObject.setClazz(DxfgEventClazz.DXFG_EVENT_SERIES.getCValue());
        return nativeObject;
    }

    @Override
    public void fillNative(final Series javaObject, final DxfgSeries nativeObject, boolean clean) {
        super.fillNative(javaObject, nativeObject, clean);
        nativeObject.setEventFlags(javaObject.getEventFlags());
        nativeObject.setIndex(javaObject.getIndex());
        nativeObject.setTimeSequence(javaObject.getTimeSequence());
        nativeObject.setExpiration(javaObject.getExpiration());
        nativeObject.setVolatility(javaObject.getVolatility());
        nativeObject.setCallVolume(javaObject.getCallVolume());
        nativeObject.setPutVolume(javaObject.getPutVolume());
        nativeObject.setPutCallRatio(javaObject.getPutCallRatio());
        nativeObject.setForwardPrice(javaObject.getForwardPrice());
        nativeObject.setDividend(javaObject.getDividend());
        nativeObject.setInterest(javaObject.getInterest());
    }

    @Override
    public void cleanNative(final DxfgSeries nativeObject) {
        super.cleanNative(nativeObject);
    }

    @Override
    protected Series doToJava(final DxfgSeries nativeObject) {
        final Series javaObject = new Series();
        this.fillJava(nativeObject, javaObject);
        return javaObject;
    }

    @Override
    public void fillJava(final DxfgSeries nativeObject, final Series javaObject) {
        super.fillJava(nativeObject, javaObject);
        javaObject.setEventFlags(nativeObject.getEventFlags());
        javaObject.setIndex(nativeObject.getIndex());
        javaObject.setTimeSequence(nativeObject.getTimeSequence());
        javaObject.setExpiration(nativeObject.getExpiration());
        javaObject.setVolatility(nativeObject.getVolatility());
        javaObject.setCallVolume(nativeObject.getCallVolume());
        javaObject.setPutVolume(nativeObject.getPutVolume());
        javaObject.setPutCallRatio(nativeObject.getPutCallRatio());
        javaObject.setForwardPrice(nativeObject.getForwardPrice());
        javaObject.setDividend(nativeObject.getDividend());
        javaObject.setInterest(nativeObject.getInterest());
    }
}
