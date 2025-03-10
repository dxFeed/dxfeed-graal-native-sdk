// Copyright (c) 2025 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

package com.dxfeed.event.market;

import com.dxfeed.sdk.events.DxfgEventClazz;
import com.dxfeed.sdk.events.DxfgOrder;
import com.dxfeed.sdk.mappers.Mapper;
import org.graalvm.nativeimage.UnmanagedMemory;
import org.graalvm.nativeimage.c.struct.SizeOf;
import org.graalvm.nativeimage.c.type.CCharPointer;

public class OrderMapper<T extends Order, V extends DxfgOrder> extends OrderAbstractMapper<T, V> {

    public OrderMapper(
            final Mapper<String, CCharPointer> stringMapper
    ) {
        super(stringMapper);
    }

    @Override
    public V createNativeObject() {
        final V nativeObject = UnmanagedMemory.calloc(SizeOf.get(DxfgOrder.class));
        nativeObject.setClazz(DxfgEventClazz.DXFG_EVENT_ORDER.getCValue());
        return nativeObject;
    }

    @Override
    public void fillNative(final T javaObject, final V nativeObject, boolean clean) {
        super.fillNative(javaObject, nativeObject, clean);
        nativeObject.setMarketMaker(stringMapper.toNative(javaObject.getMarketMaker()));
    }

    @Override
    public void cleanNative(final V nativeObject) {
        super.cleanNative(nativeObject);
        stringMapper.release(nativeObject.getMarketMaker());
    }

    @Override
    protected T doToJava(final V nativeObject) {
        final T javaObject = (T) new Order();
        this.fillJava(nativeObject, javaObject);
        return javaObject;
    }

    @Override
    public void fillJava(final V nativeObject, final T javaObject) {
        super.fillJava(nativeObject, javaObject);
        javaObject.setMarketMaker(stringMapper.toJava(nativeObject.getMarketMaker()));
    }
}
