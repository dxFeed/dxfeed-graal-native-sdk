// Copyright (c) 2025 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

package com.dxfeed.event.market;

import com.dxfeed.sdk.events.DxfgEventClazz;
import com.dxfeed.sdk.events.DxfgQuote;
import com.dxfeed.sdk.mappers.Mapper;
import org.graalvm.nativeimage.UnmanagedMemory;
import org.graalvm.nativeimage.c.struct.SizeOf;
import org.graalvm.nativeimage.c.type.CCharPointer;

public class QuoteMapper extends MarketEventMapper<Quote, DxfgQuote> {

    public QuoteMapper(
            final Mapper<String, CCharPointer> stringMapperForMarketEvent
    ) {
        super(stringMapperForMarketEvent);
    }

    @Override
    public DxfgQuote createNativeObject() {
        final DxfgQuote nativeObject = UnmanagedMemory.calloc(SizeOf.get(DxfgQuote.class));
        nativeObject.setClazz(DxfgEventClazz.DXFG_EVENT_QUOTE.getCValue());
        return nativeObject;
    }

    @Override
    public DxfgEventClazz getEventClazz() {
        return DxfgEventClazz.DXFG_EVENT_QUOTE;
    }

    @Override
    public void fillNative(final Quote javaObject, final DxfgQuote nativeObject, boolean clean) {
        super.fillNative(javaObject, nativeObject, clean);
        nativeObject.setTimeMillisSequence(javaObject.getTimeMillisSequence());
        nativeObject.setTimeNanoPart(javaObject.getTimeNanoPart());
        nativeObject.setBidTime(javaObject.getBidTime());
        nativeObject.setBidExchangeCode(javaObject.getBidExchangeCode());
        nativeObject.setBidPrice(javaObject.getBidPrice());
        nativeObject.setBidSize(javaObject.getBidSizeAsDouble());
        nativeObject.setAskTime(javaObject.getAskTime());
        nativeObject.setAskExchangeCode(javaObject.getAskExchangeCode());
        nativeObject.setAskPrice(javaObject.getAskPrice());
        nativeObject.setAskSize(javaObject.getAskSizeAsDouble());
    }

    @Override
    public void cleanNative(final DxfgQuote nativeObject) {
        super.cleanNative(nativeObject);
    }

    @Override
    protected Quote doToJava(final DxfgQuote nativeObject) {
        final Quote javaObject = new Quote();
        this.fillJava(nativeObject, javaObject);
        return javaObject;
    }

    @Override
    public void fillJava(final DxfgQuote nativeObject, final Quote javaObject) {
        super.fillJava(nativeObject, javaObject);
        javaObject.setTimeMillisSequence(nativeObject.getTimeMillisSequence());
        javaObject.setTimeNanoPart(nativeObject.getTimeNanoPart());
        javaObject.setBidTime(nativeObject.getBidTime());
        javaObject.setBidExchangeCode(nativeObject.getBidExchangeCode());
        javaObject.setBidPrice(nativeObject.getBidPrice());
        javaObject.setBidSizeAsDouble(nativeObject.getBidSize());
        javaObject.setAskTime(nativeObject.getAskTime());
        javaObject.setAskExchangeCode(nativeObject.getAskExchangeCode());
        javaObject.setAskPrice(nativeObject.getAskPrice());
        javaObject.setAskSizeAsDouble(nativeObject.getAskSize());
    }
}
