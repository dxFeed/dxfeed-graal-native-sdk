// Copyright (c) 2025 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

package com.dxfeed.event.custom;

import com.dxfeed.event.market.OrderMapper;
import com.dxfeed.sdk.events.DxfgEventClazz;
import com.dxfeed.sdk.events.DxfgNuamOrder;
import com.dxfeed.sdk.mappers.Mapper;
import org.graalvm.nativeimage.UnmanagedMemory;
import org.graalvm.nativeimage.c.struct.SizeOf;
import org.graalvm.nativeimage.c.type.CCharPointer;

public class NuamOrderMapper extends OrderMapper<NuamOrder, DxfgNuamOrder> {

    public NuamOrderMapper(
            final Mapper<String, CCharPointer> stringMapper
    ) {
        super(stringMapper);
    }

    @Override
    public DxfgNuamOrder createNativeObject() {
        final DxfgNuamOrder nativeObject = UnmanagedMemory.calloc(SizeOf.get(DxfgNuamOrder.class));

        nativeObject.setClazz(DxfgEventClazz.DXFG_EVENT_NUAM_ORDER.getCValue());

        return nativeObject;
    }

    @Override
    public DxfgEventClazz getEventClazz() {
        return DxfgEventClazz.DXFG_EVENT_NUAM_ORDER;
    }

    @SuppressWarnings("DuplicatedCode")
    @Override
    public void fillNative(final NuamOrder javaObject, final DxfgNuamOrder nativeObject, boolean clean) {
        super.fillNative(javaObject, nativeObject, clean);

        nativeObject.setActorId(javaObject.getActorId());
        nativeObject.setParticipantId(javaObject.getParticipantId());
        nativeObject.setSubmitterId(javaObject.getSubmitterId());
        nativeObject.setOnBehalfOfSubmitterId(javaObject.getOnBehalfOfSubmitterId());
        nativeObject.setClientOrderId(stringMapper.toNative(javaObject.getClientOrderId()));
        nativeObject.setCustomerAccount(stringMapper.toNative(javaObject.getCustomerAccount()));
        nativeObject.setCustomerInfo(stringMapper.toNative(javaObject.getCustomerInfo()));
        nativeObject.setExchangeInfo(stringMapper.toNative(javaObject.getExchangeInfo()));
        nativeObject.setTimeInForceData(javaObject.getTimeInForceData());
        nativeObject.setTriggerOrderBookId(javaObject.getTriggerOrderBookId());
        nativeObject.setTriggerPrice(javaObject.getTriggerPrice());
        nativeObject.setTriggerSessionType(javaObject.getTriggerSessionType());
        nativeObject.setOrderQuantity(javaObject.getOrderQuantity());
        nativeObject.setDisplayQuantity(javaObject.getDisplayQuantity());
        nativeObject.setRefreshQuantity(javaObject.getRefreshQuantity());
        nativeObject.setLeavesQuantity(javaObject.getLeavesQuantity());
        nativeObject.setMatchedQuantity(javaObject.getMatchedQuantity());
        nativeObject.setNuamFlags(javaObject.getNuamFlags());
    }

    @Override
    protected NuamOrder doToJava(final DxfgNuamOrder nativeObject) {
        final NuamOrder javaObject = new NuamOrder();

        this.fillJava(nativeObject, javaObject);

        return javaObject;
    }

    @SuppressWarnings("DuplicatedCode")
    @Override
    public void fillJava(final DxfgNuamOrder nativeObject, final NuamOrder javaObject) {
        super.fillJava(nativeObject, javaObject);

        javaObject.setActorId(nativeObject.getActorId());
        javaObject.setParticipantId(nativeObject.getParticipantId());
        javaObject.setSubmitterId(nativeObject.getSubmitterId());
        javaObject.setOnBehalfOfSubmitterId(nativeObject.getOnBehalfOfSubmitterId());
        javaObject.setClientOrderId(stringMapper.toJava(nativeObject.getClientOrderId()));
        javaObject.setCustomerAccount(stringMapper.toJava(nativeObject.getCustomerAccount()));
        javaObject.setCustomerInfo(stringMapper.toJava(nativeObject.getCustomerInfo()));
        javaObject.setExchangeInfo(stringMapper.toJava(nativeObject.getExchangeInfo()));
        javaObject.setTimeInForceData(nativeObject.getTimeInForceData());
        javaObject.setTriggerOrderBookId(nativeObject.getTriggerOrderBookId());
        javaObject.setTriggerPrice(nativeObject.getTriggerPrice());
        javaObject.setTriggerSessionType(nativeObject.getTriggerSessionType());
        javaObject.setOrderQuantity(nativeObject.getOrderQuantity());
        javaObject.setDisplayQuantity(nativeObject.getDisplayQuantity());
        javaObject.setRefreshQuantity(nativeObject.getRefreshQuantity());
        javaObject.setLeavesQuantity(nativeObject.getLeavesQuantity());
        javaObject.setMatchedQuantity(nativeObject.getMatchedQuantity());
        javaObject.setNuamFlags(nativeObject.getNuamFlags());
    }
}
