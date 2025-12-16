// Copyright (c) 2025 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

package com.dxfeed.event.custom;

import com.dxfeed.event.market.TimeAndSaleMapper;
import com.dxfeed.sdk.events.DxfgEventClazz;
import com.dxfeed.sdk.events.DxfgNuamTimeAndSale;
import com.dxfeed.sdk.mappers.Mapper;
import org.graalvm.nativeimage.UnmanagedMemory;
import org.graalvm.nativeimage.c.struct.SizeOf;
import org.graalvm.nativeimage.c.type.CCharPointer;

public class NuamTimeAndSaleMapper extends TimeAndSaleMapper<NuamTimeAndSale, DxfgNuamTimeAndSale> {

    public NuamTimeAndSaleMapper(
            final Mapper<String, CCharPointer> stringMapper
    ) {
        super(stringMapper);
    }

    @Override
    public DxfgNuamTimeAndSale createNativeObject() {
        final DxfgNuamTimeAndSale nativeObject = UnmanagedMemory.calloc(SizeOf.get(DxfgNuamTimeAndSale.class));
        nativeObject.setClazz(DxfgEventClazz.DXFG_EVENT_NUAM_TIME_AND_SALE.getCValue());

        return nativeObject;
    }

    @Override
    public DxfgEventClazz getEventClazz() {
        return DxfgEventClazz.DXFG_EVENT_NUAM_TIME_AND_SALE;
    }

    @SuppressWarnings("DuplicatedCode")
    @Override
    public void fillNative(final NuamTimeAndSale javaObject, final DxfgNuamTimeAndSale nativeObject, boolean clean) {
        super.fillNative(javaObject, nativeObject, clean);

        nativeObject.setMatchId(javaObject.getMatchId());
    }

    @Override
    protected NuamTimeAndSale doToJava(final DxfgNuamTimeAndSale nativeObject) {
        final NuamTimeAndSale javaObject = new NuamTimeAndSale();
        this.fillJava(nativeObject, javaObject);

        return javaObject;
    }

    @SuppressWarnings("DuplicatedCode")
    @Override
    public void fillJava(final DxfgNuamTimeAndSale nativeObject, final NuamTimeAndSale javaObject) {
        super.fillJava(nativeObject, javaObject);

        javaObject.setMatchId(nativeObject.getMatchId());
    }
}
