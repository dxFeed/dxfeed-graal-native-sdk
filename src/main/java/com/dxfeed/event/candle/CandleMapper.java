// Copyright (c) 2025 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

package com.dxfeed.event.candle;

import com.dxfeed.event.EventMapper;
import com.dxfeed.event.EventType;
import com.dxfeed.sdk.events.DxfgCandle;
import com.dxfeed.sdk.events.DxfgEventClazz;
import com.dxfeed.sdk.mappers.Mapper;
import com.dxfeed.sdk.symbol.DxfgSymbol;
import org.graalvm.nativeimage.UnmanagedMemory;
import org.graalvm.nativeimage.c.struct.SizeOf;
import org.graalvm.nativeimage.c.type.CCharPointer;

public class CandleMapper<T extends Candle, V extends DxfgCandle> extends EventMapper<T, V> {

    protected final Mapper<String, CCharPointer> stringMapper;
    protected final Mapper<Object, DxfgSymbol> symbolMapper;

    public CandleMapper(
            final Mapper<String, CCharPointer> stringMapperForMarketEvent,
            final Mapper<Object, DxfgSymbol> symbolMapper
    ) {
        this.symbolMapper = symbolMapper;
        this.stringMapper = stringMapperForMarketEvent;
    }

    @Override
    public V createNativeObject() {
        final V nativeObject = UnmanagedMemory.calloc(SizeOf.get(DxfgCandle.class));
        nativeObject.setClazz(DxfgEventClazz.DXFG_EVENT_CANDLE.getCValue());
        return nativeObject;
    }

    @Override
    public void fillNative(final T javaObject, final V nativeObject, boolean clean) {
        if (clean) {
            cleanNative(nativeObject);
        }

        nativeObject.setEventFlags(javaObject.getEventFlags());
        nativeObject.setEventTime(javaObject.getEventTime());
        nativeObject.setIndex(javaObject.getIndex());
        nativeObject.setCount(javaObject.getCount());
        nativeObject.setOpen(javaObject.getOpen());
        nativeObject.setHigh(javaObject.getHigh());
        nativeObject.setLow(javaObject.getLow());
        nativeObject.setClose(javaObject.getClose());
        nativeObject.setVolume(javaObject.getVolumeAsDouble());
        nativeObject.setVwap(javaObject.getVWAP());
        nativeObject.setBidVolume(javaObject.getBidVolumeAsDouble());
        nativeObject.setAskVolume(javaObject.getAskVolumeAsDouble());
        nativeObject.setImpVolatility(javaObject.getImpVolatility());
        nativeObject.setOpenInterest(javaObject.getOpenInterestAsDouble());
        nativeObject.setSymbol(this.stringMapper.toNative(javaObject.getEventSymbol().toString()));
    }

    @Override
    public void cleanNative(final V nativeObject) {
        this.stringMapper.release(nativeObject.getSymbol());
    }

    @Override
    protected T doToJava(final V nativeObject) {
        final T javaObject = (T) new Candle();
        fillJava(nativeObject, javaObject);
        return javaObject;
    }

    @Override
    public void fillJava(final V nativeObject, final T javaObject) {
        javaObject.setEventFlags(nativeObject.getEventFlags());
        javaObject.setEventTime(nativeObject.getEventTime());
        javaObject.setIndex(nativeObject.getIndex());
        javaObject.setCount(nativeObject.getCount());
        javaObject.setOpen(nativeObject.getOpen());
        javaObject.setHigh(nativeObject.getHigh());
        javaObject.setLow(nativeObject.getLow());
        javaObject.setClose(nativeObject.getClose());
        javaObject.setVolumeAsDouble(nativeObject.getVolume());
        javaObject.setVWAP(nativeObject.getVwap());
        javaObject.setBidVolumeAsDouble(nativeObject.getBidVolume());
        javaObject.setAskVolumeAsDouble(nativeObject.getAskVolume());
        javaObject.setImpVolatility(nativeObject.getImpVolatility());
        javaObject.setOpenInterestAsDouble(nativeObject.getOpenInterest());
        javaObject.setEventSymbol(CandleSymbol.valueOf(this.stringMapper.toJava(nativeObject.getSymbol())));
    }

    @Override
    public V createNativeObject(final String symbol) {
        final V nativeObject = createNativeObject();
        nativeObject.setSymbol(this.stringMapper.toNative(symbol));
        return nativeObject;
    }

    @Override
    public DxfgEventClazz getEventClazz() {
        return DxfgEventClazz.DXFG_EVENT_CANDLE;
    }
}
