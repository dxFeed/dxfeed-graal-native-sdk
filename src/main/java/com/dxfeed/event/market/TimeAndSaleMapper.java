// Copyright (c) 2025 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

package com.dxfeed.event.market;

import com.dxfeed.sdk.events.DxfgEventClazz;
import com.dxfeed.sdk.events.DxfgTimeAndSale;
import com.dxfeed.sdk.mappers.Mapper;
import org.graalvm.nativeimage.UnmanagedMemory;
import org.graalvm.nativeimage.c.struct.SizeOf;
import org.graalvm.nativeimage.c.type.CCharPointer;

public class TimeAndSaleMapper<JavaObjectType extends TimeAndSale, NativeObjectType extends DxfgTimeAndSale> extends
        MarketEventMapper<JavaObjectType, NativeObjectType> {

    public TimeAndSaleMapper(
            final Mapper<String, CCharPointer> stringMapper
    ) {
        super(stringMapper);
    }

    @Override
    public NativeObjectType createNativeObject() {
        final NativeObjectType nativeObject = UnmanagedMemory.calloc(SizeOf.get(DxfgTimeAndSale.class));
        nativeObject.setClazz(DxfgEventClazz.DXFG_EVENT_TIME_AND_SALE.getCValue());

        return nativeObject;
    }

    @Override
    public DxfgEventClazz getEventClazz() {
        return DxfgEventClazz.DXFG_EVENT_TIME_AND_SALE;
    }

    @Override
    public void fillNative(final JavaObjectType javaObject, final NativeObjectType nativeObject, boolean clean) {
        super.fillNative(javaObject, nativeObject, clean);
        nativeObject.setEventFlags(javaObject.getEventFlags());
        nativeObject.setIndex(javaObject.getIndex());
        nativeObject.setTimeNanoPart(javaObject.getTimeNanoPart());
        nativeObject.setExchangeCode(javaObject.getExchangeCode());
        nativeObject.setPrice(javaObject.getPrice());
        nativeObject.setSize(javaObject.getSizeAsDouble());
        nativeObject.setBidPrice(javaObject.getBidPrice());
        nativeObject.setAskPrice(javaObject.getAskPrice());
        nativeObject.setExchangeSaleConditions(
                stringMapper.toNative(javaObject.getExchangeSaleConditions())
        );
        nativeObject.setFlags(javaObject.getFlags());
        nativeObject.setBuyer(stringMapper.toNative(javaObject.getBuyer()));
        nativeObject.setSeller(stringMapper.toNative(javaObject.getSeller()));
        nativeObject.setTradeId(javaObject.getTradeId());
    }

    @Override
    public void cleanNative(final NativeObjectType nativeObject) {
        super.cleanNative(nativeObject);

        stringMapper.release(nativeObject.getExchangeSaleConditions());
        stringMapper.release(nativeObject.getBuyer());
        stringMapper.release(nativeObject.getSeller());
    }

    @Override
    protected JavaObjectType doToJava(final NativeObjectType nativeObject) {
        @SuppressWarnings("unchecked") final JavaObjectType javaObject = (JavaObjectType) new TimeAndSale();
        this.fillJava(nativeObject, javaObject);

        return javaObject;
    }

    @Override
    public void fillJava(final NativeObjectType nativeObject, final JavaObjectType javaObject) {
        super.fillJava(nativeObject, javaObject);

        javaObject.setEventFlags(nativeObject.getEventFlags());
        javaObject.setIndex(nativeObject.getIndex());
        javaObject.setTimeNanoPart(nativeObject.getTimeNanoPart());
        javaObject.setExchangeCode(nativeObject.getExchangeCode());
        javaObject.setPrice(nativeObject.getPrice());
        javaObject.setSizeAsDouble(nativeObject.getSize());
        javaObject.setBidPrice(nativeObject.getBidPrice());
        javaObject.setAskPrice(nativeObject.getAskPrice());
        javaObject.setExchangeSaleConditions(
                stringMapper.toJava(nativeObject.getExchangeSaleConditions())
        );
        javaObject.setFlags(nativeObject.getFlags());
        javaObject.setBuyer(stringMapper.toJava(nativeObject.getBuyer()));
        javaObject.setSeller(stringMapper.toJava(nativeObject.getSeller()));
        javaObject.setTradeId(nativeObject.getTradeId());
    }
}
