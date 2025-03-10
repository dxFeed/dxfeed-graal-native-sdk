// Copyright (c) 2025 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

package com.dxfeed.event.market;

import com.dxfeed.sdk.events.DxfgOrderBase;
import com.dxfeed.sdk.mappers.Mapper;
import org.graalvm.nativeimage.c.type.CCharPointer;

public abstract class OrderAbstractMapper<V extends OrderBase, T extends DxfgOrderBase>
        extends MarketEventMapper<V, T> {

    public OrderAbstractMapper(
            final Mapper<String, CCharPointer> stringMapper
    ) {
        super(stringMapper);
    }

    @Override
    public void fillNative(final V javaObject, final T nativeObject, boolean clean) {
        super.fillNative(javaObject, nativeObject, clean);
        nativeObject.setEventFlags(javaObject.getEventFlags());
        nativeObject.setIndex(javaObject.getIndex());
        nativeObject.setTimeSequence(javaObject.getTimeSequence());
        nativeObject.setTimeNanoPart(javaObject.getTimeNanoPart());
        nativeObject.setActionTime(javaObject.getActionTime());
        nativeObject.setOrderId(javaObject.getOrderId());
        nativeObject.setAuxOrderId(javaObject.getAuxOrderId());
        nativeObject.setPrice(javaObject.getPrice());
        nativeObject.setSize(javaObject.getSizeAsDouble());
        nativeObject.setExecutedSize(javaObject.getExecutedSize());
        nativeObject.setCount(javaObject.getCount());
        nativeObject.setFlags(javaObject.getFlags());
        nativeObject.setTradeId(javaObject.getTradeId());
        nativeObject.setTradePrice(javaObject.getTradePrice());
        nativeObject.setTradeSize(javaObject.getTradeSize());
    }

    @Override
    public void cleanNative(final T nativeObject) {
        super.cleanNative(nativeObject);
    }

    @Override
    public void fillJava(final T nativeObject, final V javaObject) {
        super.fillJava(nativeObject, javaObject);
        javaObject.setEventFlags(nativeObject.getEventFlags());
        javaObject.setIndex(nativeObject.getIndex());
        javaObject.setTimeSequence(nativeObject.getTimeSequence());
        javaObject.setTimeNanoPart(nativeObject.getTimeNanoPart());
        javaObject.setActionTime(nativeObject.getActionTime());
        javaObject.setOrderId(nativeObject.getOrderId());
        javaObject.setAuxOrderId(nativeObject.getAuxOrderId());
        javaObject.setPrice(nativeObject.getPrice());
        javaObject.setSizeAsDouble(nativeObject.getSize());
        javaObject.setExecutedSize(nativeObject.getExecutedSize());
        javaObject.setCount(nativeObject.getCount());
        javaObject.setFlags(nativeObject.getFlags());
        javaObject.setTradeId(nativeObject.getTradeId());
        javaObject.setTradePrice(nativeObject.getTradePrice());
        javaObject.setTradeSize(nativeObject.getTradeSize());
    }
}
