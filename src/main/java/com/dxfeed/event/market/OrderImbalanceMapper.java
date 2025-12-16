// Copyright (c) 2025 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

package com.dxfeed.event.market;

import com.dxfeed.sdk.events.DxfgEventClazz;
import com.dxfeed.sdk.events.DxfgOrderImbalance;
import com.dxfeed.sdk.mappers.Mapper;
import org.graalvm.nativeimage.UnmanagedMemory;
import org.graalvm.nativeimage.c.struct.SizeOf;
import org.graalvm.nativeimage.c.type.CCharPointer;

public class OrderImbalanceMapper extends MarketEventMapper<OrderImbalance, DxfgOrderImbalance> {

    public OrderImbalanceMapper(
            final Mapper<String, CCharPointer> stringMapperForMarketEvent
    ) {
        super(stringMapperForMarketEvent);
    }

    @Override
    public DxfgOrderImbalance createNativeObject() {
        final DxfgOrderImbalance nativeObject = UnmanagedMemory.calloc(SizeOf.get(DxfgOrderImbalance.class));

        nativeObject.setClazz(DxfgEventClazz.DXFG_EVENT_ORDER_IMBALANCE.getCValue());

        return nativeObject;
    }

    @Override
    public DxfgEventClazz getEventClazz() {
        return DxfgEventClazz.DXFG_EVENT_ORDER_IMBALANCE;
    }

    @SuppressWarnings("DuplicatedCode")
    @Override
    public void fillNative(final OrderImbalance javaObject, final DxfgOrderImbalance nativeObject, boolean clean) {
        super.fillNative(javaObject, nativeObject, clean);

        nativeObject.setSourceId(javaObject.getSource().id());
        nativeObject.setTimeSequence(javaObject.getTimeSequence());
        nativeObject.setRefPrice(javaObject.getRefPrice());
        nativeObject.setPairedSize(javaObject.getPairedSize());
        nativeObject.setImbalanceSize(javaObject.getImbalanceSize());
        nativeObject.setNearPrice(javaObject.getNearPrice());
        nativeObject.setFarPrice(javaObject.getFarPrice());
        nativeObject.setFlags(javaObject.getFlags());
    }

    @Override
    public void cleanNative(final DxfgOrderImbalance nativeObject) {
        super.cleanNative(nativeObject);
    }

    @Override
    protected OrderImbalance doToJava(final DxfgOrderImbalance nativeObject) {
        final OrderImbalance javaObject = new OrderImbalance();

        this.fillJava(nativeObject, javaObject);

        return javaObject;
    }

    @SuppressWarnings("DuplicatedCode")
    @Override
    public void fillJava(final DxfgOrderImbalance nativeObject, final OrderImbalance javaObject) {
        super.fillJava(nativeObject, javaObject);

        javaObject.setSource(OrderSource.valueOf(nativeObject.getSourceId()));
        javaObject.setTimeSequence(nativeObject.getTimeSequence());
        javaObject.setRefPrice(nativeObject.getRefPrice());
        javaObject.setPairedSize(nativeObject.getPairedSize());
        javaObject.setImbalanceSize(nativeObject.getImbalanceSize());
        javaObject.setNearPrice(nativeObject.getNearPrice());
        javaObject.setFarPrice(nativeObject.getFarPrice());
        javaObject.setFlags(nativeObject.getFlags());
    }
}
