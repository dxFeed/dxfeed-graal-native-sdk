// Copyright (c) 2025 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

package com.dxfeed.event.market;

import com.dxfeed.sdk.events.DxfgEventClazz;
import com.dxfeed.sdk.events.DxfgSummary;
import com.dxfeed.sdk.mappers.Mapper;
import org.graalvm.nativeimage.UnmanagedMemory;
import org.graalvm.nativeimage.c.struct.SizeOf;
import org.graalvm.nativeimage.c.type.CCharPointer;

public class SummaryMapper extends MarketEventMapper<Summary, DxfgSummary> {

    public SummaryMapper(
            final Mapper<String, CCharPointer> stringMapperForMarketEvent
    ) {
        super(stringMapperForMarketEvent);
    }

    @Override
    public DxfgSummary createNativeObject() {
        final DxfgSummary nativeObject = UnmanagedMemory.calloc(SizeOf.get(DxfgSummary.class));
        nativeObject.setClazz(DxfgEventClazz.DXFG_EVENT_SUMMARY.getCValue());
        return nativeObject;
    }

    @Override
    public DxfgEventClazz getEventClazz() {
        return DxfgEventClazz.DXFG_EVENT_SUMMARY;
    }

    @Override
    public void fillNative(final Summary javaObject, final DxfgSummary nativeObject, boolean clean) {
        super.fillNative(javaObject, nativeObject, clean);
        nativeObject.setDayId(javaObject.getDayId());
        nativeObject.setDayOpenPrice(javaObject.getDayOpenPrice());
        nativeObject.setDayHighPrice(javaObject.getDayHighPrice());
        nativeObject.setDayLowPrice(javaObject.getDayLowPrice());
        nativeObject.setDayClosePrice(javaObject.getDayClosePrice());
        nativeObject.setPrevDayId(javaObject.getPrevDayId());
        nativeObject.setPrevDayClosePrice(javaObject.getPrevDayClosePrice());
        nativeObject.setPrevDayVolume(javaObject.getPrevDayVolume());
        nativeObject.setOpenInterest(javaObject.getOpenInterest());
        nativeObject.setFlags(javaObject.getFlags());
    }

    @Override
    public void cleanNative(final DxfgSummary nativeObject) {
        super.cleanNative(nativeObject);
    }

    @Override
    protected Summary doToJava(final DxfgSummary nativeObject) {
        final Summary javaObject = new Summary();
        this.fillJava(nativeObject, javaObject);
        return javaObject;
    }

    @Override
    public void fillJava(final DxfgSummary nativeObject, final Summary javaObject) {
        super.fillJava(nativeObject, javaObject);
        javaObject.setDayId(nativeObject.getDayId());
        javaObject.setDayOpenPrice(nativeObject.getDayOpenPrice());
        javaObject.setDayHighPrice(nativeObject.getDayHighPrice());
        javaObject.setDayLowPrice(nativeObject.getDayLowPrice());
        javaObject.setDayClosePrice(nativeObject.getDayClosePrice());
        javaObject.setPrevDayId(nativeObject.getPrevDayId());
        javaObject.setPrevDayClosePrice(nativeObject.getPrevDayClosePrice());
        javaObject.setPrevDayVolume(nativeObject.getPrevDayVolume());
        javaObject.setOpenInterest(nativeObject.getOpenInterest());
        javaObject.setFlags(nativeObject.getFlags());
    }
}
