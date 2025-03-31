// Copyright (c) 2025 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

package com.dxfeed.event.candle;

import com.dxfeed.sdk.events.DxfgDailyCandle;
import com.dxfeed.sdk.events.DxfgEventClazz;
import com.dxfeed.sdk.mappers.Mapper;
import com.dxfeed.sdk.symbol.DxfgSymbol;
import org.graalvm.nativeimage.UnmanagedMemory;
import org.graalvm.nativeimage.c.struct.SizeOf;
import org.graalvm.nativeimage.c.type.CCharPointer;

public class DailyCandleMapper extends CandleMapper<DailyCandle, DxfgDailyCandle> {

    public DailyCandleMapper(
            final Mapper<String, CCharPointer> stringMapperForMarketEvent,
            final Mapper<Object, DxfgSymbol> symbolMapper
    ) {
        super(stringMapperForMarketEvent, symbolMapper);
    }

    @Override
    public DxfgDailyCandle createNativeObject() {
        final DxfgDailyCandle nativeObject = UnmanagedMemory.calloc(SizeOf.get(DxfgDailyCandle.class));
        nativeObject.setClazz(DxfgEventClazz.DXFG_EVENT_DAILY_CANDLE.getCValue());
        return nativeObject;
    }

    @Override
    protected DailyCandle doToJava(final DxfgDailyCandle nativeObject) {
        final DailyCandle javaObject = new DailyCandle();
        fillJava(nativeObject, javaObject);
        return javaObject;
    }

    @Override
    public DxfgEventClazz getEventClazz() {
        return DxfgEventClazz.DXFG_EVENT_DAILY_CANDLE;
    }
}
