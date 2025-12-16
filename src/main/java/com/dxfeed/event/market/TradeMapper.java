// Copyright (c) 2025 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

package com.dxfeed.event.market;

import com.dxfeed.sdk.events.DxfgEventClazz;
import com.dxfeed.sdk.events.DxfgTrade;
import com.dxfeed.sdk.mappers.Mapper;
import org.graalvm.nativeimage.UnmanagedMemory;
import org.graalvm.nativeimage.c.struct.SizeOf;
import org.graalvm.nativeimage.c.type.CCharPointer;

public class TradeMapper<JavaObjectType extends Trade, NativeObjectType extends DxfgTrade> extends
        TradeBaseMapper<JavaObjectType, NativeObjectType> {

    public TradeMapper(final Mapper<String, CCharPointer> stringMapperForMarketEvent) {
        super(stringMapperForMarketEvent);
    }

    @Override
    public NativeObjectType createNativeObject() {
        final NativeObjectType nativeObject = UnmanagedMemory.calloc(SizeOf.get(DxfgTrade.class));
        nativeObject.setClazz(DxfgEventClazz.DXFG_EVENT_TRADE.getCValue());

        return nativeObject;
    }

    @Override
    public DxfgEventClazz getEventClazz() {
        return DxfgEventClazz.DXFG_EVENT_TRADE;
    }

    @Override
    public void fillNative(final JavaObjectType javaObject, final NativeObjectType nativeObject, boolean clean) {
        super.fillNative(javaObject, nativeObject, clean);
    }

    @Override
    public void cleanNative(final NativeObjectType nativeObject) {
        super.cleanNative(nativeObject);
    }

    @Override
    protected JavaObjectType doToJava(final NativeObjectType nativeObject) {
        @SuppressWarnings("unchecked") final JavaObjectType javaObject = (JavaObjectType) new Trade();
        this.fillJava(nativeObject, javaObject);

        return javaObject;
    }

    @Override
    public void fillJava(final NativeObjectType nativeObject, final JavaObjectType javaObject) {
        super.fillJava(nativeObject, javaObject);
    }
}
