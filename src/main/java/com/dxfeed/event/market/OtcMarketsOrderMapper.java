// Copyright (c) 2025 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

package com.dxfeed.event.market;

import com.dxfeed.sdk.events.DxfgEventClazz;
import com.dxfeed.sdk.events.DxfgOtcMarketsOrder;
import com.dxfeed.sdk.mappers.Mapper;
import org.graalvm.nativeimage.UnmanagedMemory;
import org.graalvm.nativeimage.c.struct.SizeOf;
import org.graalvm.nativeimage.c.type.CCharPointer;

public class OtcMarketsOrderMapper extends OrderMapper<OtcMarketsOrder, DxfgOtcMarketsOrder> {

    public OtcMarketsOrderMapper(
            final Mapper<String, CCharPointer> stringMapper
    ) {
        super(stringMapper);
    }

    @Override
    public DxfgOtcMarketsOrder createNativeObject() {
        final DxfgOtcMarketsOrder nativeObject = UnmanagedMemory.calloc(SizeOf.get(DxfgOtcMarketsOrder.class));
        nativeObject.setClazz(DxfgEventClazz.DXFG_EVENT_OTC_MARKETS_ORDER.getCValue());
        return nativeObject;
    }

    @Override
    public void fillNative(final OtcMarketsOrder javaObject, final DxfgOtcMarketsOrder nativeObject, boolean clean) {
        super.fillNative(javaObject, nativeObject, clean);
        nativeObject.setQuoteAccessPayment(javaObject.getQuoteAccessPayment());
        nativeObject.setOtcMarketsFlags(javaObject.getOtcMarketsFlags());
    }

    @Override
    protected OtcMarketsOrder doToJava(final DxfgOtcMarketsOrder nativeObject) {
        final OtcMarketsOrder javaObject = new OtcMarketsOrder();
        this.fillJava(nativeObject, javaObject);
        return javaObject;
    }

    @Override
    public void fillJava(final DxfgOtcMarketsOrder nativeObject, final OtcMarketsOrder javaObject) {
        super.fillJava(nativeObject, javaObject);
        javaObject.setQuoteAccessPayment(nativeObject.getQuoteAccessPayment());
        javaObject.setOtcMarketsFlags(nativeObject.getOtcMarketsFlags());
    }
}
