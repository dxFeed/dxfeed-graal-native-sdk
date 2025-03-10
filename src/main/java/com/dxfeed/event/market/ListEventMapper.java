// Copyright (c) 2025 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

package com.dxfeed.event.market;

import com.dxfeed.event.EventType;
import com.dxfeed.sdk.events.DxfgEventType;
import com.dxfeed.sdk.events.DxfgEventTypeList;
import com.dxfeed.sdk.events.DxfgEventTypePointer;
import com.dxfeed.sdk.mappers.ListMapper;
import com.dxfeed.sdk.mappers.Mapper;
import org.graalvm.nativeimage.c.struct.SizeOf;

public class ListEventMapper extends
        ListMapper<EventType<?>, DxfgEventType, DxfgEventTypePointer, DxfgEventTypeList> {

    private final Mapper<EventType<?>, DxfgEventType> eventMappers;

    public ListEventMapper(final Mapper<EventType<?>, DxfgEventType> eventMappers) {
        this.eventMappers = eventMappers;
    }

    @Override
    protected EventType<?> toJava(final DxfgEventType nativeObject) {
        return eventMappers.toJava(nativeObject);
    }

    @Override
    protected void releaseNative(final DxfgEventType nativeObject) {
        eventMappers.release(nativeObject);
    }

    @Override
    protected DxfgEventType toNative(final EventType<?> javaObject) {
        return eventMappers.toNative(javaObject);
    }

    @Override
    protected int getNativeListSize() {
        return SizeOf.get(DxfgEventTypeList.class);
    }
}
