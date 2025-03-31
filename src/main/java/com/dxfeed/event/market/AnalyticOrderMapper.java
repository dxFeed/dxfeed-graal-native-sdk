// Copyright (c) 2025 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

package com.dxfeed.event.market;

import com.dxfeed.sdk.events.DxfgAnalyticOrder;
import com.dxfeed.sdk.events.DxfgEventClazz;
import com.dxfeed.sdk.mappers.Mapper;
import org.graalvm.nativeimage.UnmanagedMemory;
import org.graalvm.nativeimage.c.struct.SizeOf;
import org.graalvm.nativeimage.c.type.CCharPointer;

public class AnalyticOrderMapper extends OrderMapper<AnalyticOrder, DxfgAnalyticOrder> {

    public AnalyticOrderMapper(
            final Mapper<String, CCharPointer> stringMapper
    ) {
        super(stringMapper);
    }

    @Override
    public DxfgAnalyticOrder createNativeObject() {
        final DxfgAnalyticOrder nativeObject = UnmanagedMemory.calloc(SizeOf.get(DxfgAnalyticOrder.class));
        nativeObject.setClazz(DxfgEventClazz.DXFG_EVENT_ANALYTIC_ORDER.getCValue());
        return nativeObject;
    }

    @Override
    public DxfgEventClazz getEventClazz() {
        return DxfgEventClazz.DXFG_EVENT_ANALYTIC_ORDER;
    }

    @Override
    public void fillNative(final AnalyticOrder javaObject, final DxfgAnalyticOrder nativeObject, boolean clean) {
        super.fillNative(javaObject, nativeObject, clean);
        nativeObject.setIcebergPeakSize(javaObject.getIcebergPeakSize());
        nativeObject.setIcebergHiddenSize(javaObject.getIcebergHiddenSize());
        nativeObject.setIcebergExecutedSize(javaObject.getIcebergExecutedSize());
        nativeObject.setIcebergFlags(javaObject.getIcebergFlags());
    }

    @Override
    protected AnalyticOrder doToJava(final DxfgAnalyticOrder nativeObject) {
        final AnalyticOrder javaObject = new AnalyticOrder();
        this.fillJava(nativeObject, javaObject);
        return javaObject;
    }

    @Override
    public void fillJava(final DxfgAnalyticOrder nativeObject, final AnalyticOrder javaObject) {
        super.fillJava(nativeObject, javaObject);
        javaObject.setIcebergPeakSize(nativeObject.getIcebergPeakSize());
        javaObject.setIcebergHiddenSize(nativeObject.getIcebergHiddenSize());
        javaObject.setIcebergExecutedSize(nativeObject.getIcebergExecutedSize());
        javaObject.setIcebergFlags(nativeObject.getIcebergFlags());
    }
}
