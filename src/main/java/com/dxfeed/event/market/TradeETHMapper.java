// Copyright (c) 2025 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

package com.dxfeed.event.market;

import com.dxfeed.sdk.events.DxfgEventClazz;
import com.dxfeed.sdk.events.DxfgTradeETH;
import com.dxfeed.sdk.mappers.Mapper;
import org.graalvm.nativeimage.UnmanagedMemory;
import org.graalvm.nativeimage.c.struct.SizeOf;
import org.graalvm.nativeimage.c.type.CCharPointer;

public class TradeETHMapper extends TradeBaseMapper<TradeETH, DxfgTradeETH> {

    public TradeETHMapper(final Mapper<String, CCharPointer> stringMapperForMarketEvent) {
        super(stringMapperForMarketEvent);
    }

    @Override
    public DxfgTradeETH createNativeObject() {
        final DxfgTradeETH nativeObject = UnmanagedMemory.calloc(SizeOf.get(DxfgTradeETH.class));
        nativeObject.setClazz(DxfgEventClazz.DXFG_EVENT_TRADE_ETH.getCValue());
        return nativeObject;
    }

    @Override
    public DxfgEventClazz getEventClazz() {
        return DxfgEventClazz.DXFG_EVENT_TRADE_ETH;
    }

    @Override
    public void fillNative(final TradeETH javaObject, final DxfgTradeETH nativeObject, boolean clean) {
        super.fillNative(javaObject, nativeObject, clean);
    }

    @Override
    public void cleanNative(final DxfgTradeETH nativeObject) {
        super.cleanNative(nativeObject);
    }

    @Override
    protected TradeETH doToJava(final DxfgTradeETH nativeObject) {
        final TradeETH javaObject = new TradeETH();
        this.fillJava(nativeObject, javaObject);
        return javaObject;
    }

    @Override
    public void fillJava(final DxfgTradeETH nativeObject, final TradeETH javaObject) {
        super.fillJava(nativeObject, javaObject);
    }
}
