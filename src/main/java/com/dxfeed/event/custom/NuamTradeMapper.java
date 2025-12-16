// Copyright (c) 2025 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

package com.dxfeed.event.custom;

import com.dxfeed.event.market.TradeMapper;
import com.dxfeed.sdk.events.DxfgEventClazz;
import com.dxfeed.sdk.events.DxfgNuamTrade;
import com.dxfeed.sdk.mappers.Mapper;
import org.graalvm.nativeimage.UnmanagedMemory;
import org.graalvm.nativeimage.c.struct.SizeOf;
import org.graalvm.nativeimage.c.type.CCharPointer;

public class NuamTradeMapper extends TradeMapper<NuamTrade, DxfgNuamTrade> {

    public NuamTradeMapper(
            final Mapper<String, CCharPointer> stringMapper
    ) {
        super(stringMapper);
    }

    @Override
    public DxfgNuamTrade createNativeObject() {
        final DxfgNuamTrade nativeObject = UnmanagedMemory.calloc(SizeOf.get(DxfgNuamTrade.class));
        nativeObject.setClazz(DxfgEventClazz.DXFG_EVENT_NUAM_TRADE.getCValue());

        return nativeObject;
    }

    @Override
    public DxfgEventClazz getEventClazz() {
        return DxfgEventClazz.DXFG_EVENT_NUAM_TRADE;
    }

    @SuppressWarnings("DuplicatedCode")
    @Override
    public void fillNative(final NuamTrade javaObject, final DxfgNuamTrade nativeObject, boolean clean) {
        super.fillNative(javaObject, nativeObject, clean);

        nativeObject.setTradeStatTime(javaObject.getTradeStatTime());
        nativeObject.setLastSignificantPrice(javaObject.getLastSignificantPrice());
        nativeObject.setLastPriceForAll(javaObject.getLastPriceForAll());
        nativeObject.setNumberOfTrades(javaObject.getNumberOfTrades());
        nativeObject.setVWAP(javaObject.getVWAP());
    }

    @Override
    protected NuamTrade doToJava(final DxfgNuamTrade nativeObject) {
        final NuamTrade javaObject = new NuamTrade();
        this.fillJava(nativeObject, javaObject);

        return javaObject;
    }

    @SuppressWarnings("DuplicatedCode")
    @Override
    public void fillJava(final DxfgNuamTrade nativeObject, final NuamTrade javaObject) {
        super.fillJava(nativeObject, javaObject);

        javaObject.setTradeStatTime(nativeObject.getTradeStatTime());
        javaObject.setLastSignificantPrice(nativeObject.getLastSignificantPrice());
        javaObject.setLastPriceForAll(nativeObject.getLastPriceForAll());
        javaObject.setNumberOfTrades(nativeObject.getNumberOfTrades());
        javaObject.setVWAP(nativeObject.getVWAP());
    }
}
