// Copyright (c) 2025 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

package com.dxfeed.event.market;

import com.dxfeed.sdk.events.DxfgEventClazz;
import com.dxfeed.sdk.events.DxfgSpreadOrder;
import com.dxfeed.sdk.mappers.Mapper;
import org.graalvm.nativeimage.UnmanagedMemory;
import org.graalvm.nativeimage.c.struct.SizeOf;
import org.graalvm.nativeimage.c.type.CCharPointer;

public class SpreadOrderMapper extends OrderAbstractMapper<SpreadOrder, DxfgSpreadOrder> {

    public SpreadOrderMapper(
            final Mapper<String, CCharPointer> stringMapper
    ) {
        super(stringMapper);
    }

    @Override
    public DxfgSpreadOrder createNativeObject() {
        final DxfgSpreadOrder nativeObject = UnmanagedMemory.calloc(SizeOf.get(DxfgSpreadOrder.class));
        nativeObject.setClazz(DxfgEventClazz.DXFG_EVENT_SPREAD_ORDER.getCValue());
        return nativeObject;
    }

    @Override
    public DxfgEventClazz getEventClazz() {
        return DxfgEventClazz.DXFG_EVENT_SPREAD_ORDER;
    }

    @Override
    public void fillNative(final SpreadOrder javaObject, final DxfgSpreadOrder nativeObject, boolean clean) {
        super.fillNative(javaObject, nativeObject, clean);
        nativeObject.setSpreadSymbol(stringMapper.toNative(javaObject.getSpreadSymbol()));
    }

    @Override
    public void cleanNative(final DxfgSpreadOrder nativeObject) {
        super.cleanNative(nativeObject);
        stringMapper.release(nativeObject.getSpreadSymbol());
    }

    @Override
    protected SpreadOrder doToJava(final DxfgSpreadOrder nativeObject) {
        final SpreadOrder javaObject = new SpreadOrder();
        this.fillJava(nativeObject, javaObject);
        return javaObject;
    }

    @Override
    public void fillJava(final DxfgSpreadOrder nativeObject, final SpreadOrder javaObject) {
        super.fillJava(nativeObject, javaObject);
        javaObject.setSpreadSymbol(stringMapper.toJava(nativeObject.getSpreadSymbol()));
    }
}
