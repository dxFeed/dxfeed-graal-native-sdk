// Copyright (c) 2025 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

package com.dxfeed.event.market;

import com.dxfeed.sdk.events.DxfgEventClazz;
import com.dxfeed.sdk.events.DxfgOrderBase;
import com.dxfeed.sdk.mappers.Mapper;
import org.graalvm.nativeimage.UnmanagedMemory;
import org.graalvm.nativeimage.c.struct.SizeOf;
import org.graalvm.nativeimage.c.type.CCharPointer;

public class OrderBaseMapper extends OrderAbstractMapper<OrderBase, DxfgOrderBase> {

    public OrderBaseMapper(final Mapper<String, CCharPointer> stringMapperForMarketEvent) {
        super(stringMapperForMarketEvent);
    }

    @Override
    public DxfgOrderBase createNativeObject() {
        final DxfgOrderBase nativeObject = UnmanagedMemory.calloc(SizeOf.get(DxfgOrderBase.class));
        nativeObject.setClazz(DxfgEventClazz.DXFG_EVENT_ORDER_BASE.getCValue());
        return nativeObject;
    }

    @Override
    public DxfgEventClazz getEventClazz() {
        return DxfgEventClazz.DXFG_EVENT_ORDER_BASE;
    }

    @Override
    public void fillNative(final OrderBase javaObject, final DxfgOrderBase nativeObject, boolean clean) {
        super.fillNative(javaObject, nativeObject, clean);
    }

    @Override
    public void cleanNative(final DxfgOrderBase nativeObject) {
        super.cleanNative(nativeObject);
    }

    @Override
    protected OrderBase doToJava(final DxfgOrderBase nativeObject) {
        final OrderBase javaObject = new OrderBase();
        this.fillJava(nativeObject, javaObject);
        return javaObject;
    }

    @Override
    public void fillJava(final DxfgOrderBase nativeObject, final OrderBase javaObject) {
        super.fillJava(nativeObject, javaObject);
    }
}
