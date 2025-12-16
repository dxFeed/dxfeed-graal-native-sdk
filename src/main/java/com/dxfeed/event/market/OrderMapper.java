// Copyright (c) 2025 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

package com.dxfeed.event.market;

import com.dxfeed.sdk.events.DxfgEventClazz;
import com.dxfeed.sdk.events.DxfgOrder;
import com.dxfeed.sdk.mappers.Mapper;
import org.graalvm.nativeimage.UnmanagedMemory;
import org.graalvm.nativeimage.c.struct.SizeOf;
import org.graalvm.nativeimage.c.type.CCharPointer;

public class OrderMapper<JavaObjectType extends Order, NativeObjectType extends DxfgOrder> extends
        OrderAbstractMapper<JavaObjectType, NativeObjectType> {

    public OrderMapper(
            final Mapper<String, CCharPointer> stringMapper
    ) {
        super(stringMapper);
    }

    @Override
    public NativeObjectType createNativeObject() {
        final NativeObjectType nativeObject = UnmanagedMemory.calloc(SizeOf.get(DxfgOrder.class));
        nativeObject.setClazz(DxfgEventClazz.DXFG_EVENT_ORDER.getCValue());
        return nativeObject;
    }

    @Override
    public DxfgEventClazz getEventClazz() {
        return DxfgEventClazz.DXFG_EVENT_ORDER;
    }

    @Override
    public void fillNative(final JavaObjectType javaObject, final NativeObjectType nativeObject, boolean clean) {
        super.fillNative(javaObject, nativeObject, clean);
        nativeObject.setMarketMaker(stringMapper.toNative(javaObject.getMarketMaker()));
    }

    @Override
    public void cleanNative(final NativeObjectType nativeObject) {
        super.cleanNative(nativeObject);
        stringMapper.release(nativeObject.getMarketMaker());
    }

    @Override
    protected JavaObjectType doToJava(final NativeObjectType nativeObject) {
        @SuppressWarnings("unchecked") final JavaObjectType javaObject = (JavaObjectType) new Order();
        this.fillJava(nativeObject, javaObject);
        return javaObject;
    }

    @Override
    public void fillJava(final NativeObjectType nativeObject, final JavaObjectType javaObject) {
        super.fillJava(nativeObject, javaObject);
        javaObject.setMarketMaker(stringMapper.toJava(nativeObject.getMarketMaker()));
    }
}
