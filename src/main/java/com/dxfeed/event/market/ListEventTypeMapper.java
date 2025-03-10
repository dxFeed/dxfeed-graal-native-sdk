// Copyright (c) 2025 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

package com.dxfeed.event.market;

import com.dxfeed.event.EventType;
import com.dxfeed.sdk.events.DxfgEventClazz;
import com.dxfeed.sdk.events.DxfgEventClazzList;
import com.dxfeed.sdk.events.DxfgEventClazzPointer;
import com.dxfeed.sdk.mappers.ListMapper;
import org.graalvm.nativeimage.UnmanagedMemory;
import org.graalvm.nativeimage.c.struct.SizeOf;
import org.graalvm.nativeimage.c.type.CIntPointer;

public class ListEventTypeMapper extends
        ListMapper<Class<? extends EventType<?>>, CIntPointer, DxfgEventClazzPointer, DxfgEventClazzList> {

    @Override
    protected Class<? extends EventType<?>> toJava(final CIntPointer nativeObject) {
        return DxfgEventClazz.fromCValue(nativeObject.read()).clazz;
    }

    @Override
    protected CIntPointer toNative(final Class<? extends EventType<?>> javaObject) {
        final CIntPointer cIntPointer = UnmanagedMemory.calloc(SIZE_OF_C_POINTER);

        cIntPointer.write(DxfgEventClazz.of(javaObject).getCValue());

        return cIntPointer;
    }

    @Override
    protected void releaseNative(final CIntPointer nativeObject) {
        UnmanagedMemory.free(nativeObject);
    }

    @Override
    protected int getNativeListSize() {
        return SizeOf.get(DxfgEventClazzList.class);
    }
}
