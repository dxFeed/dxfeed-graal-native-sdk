// Copyright (c) 2025 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

package com.dxfeed.event;

import com.dxfeed.sdk.events.DxfgEventClazz;
import com.dxfeed.sdk.events.DxfgEventClazzList;
import com.dxfeed.sdk.events.DxfgEventClazzPointer;
import com.dxfeed.sdk.mappers.ListMapper;
import java.util.List;
import org.graalvm.nativeimage.UnmanagedMemory;
import org.graalvm.nativeimage.c.struct.SizeOf;
import org.graalvm.nativeimage.c.type.CIntPointer;

public class ListEventTypeMapper extends
        ListMapper<Class<? extends EventType<?>>, CIntPointer, DxfgEventClazzPointer, DxfgEventClazzList> {

    /**
     * The classes as an array for the varargs of the Java API ({@code createSubscription(Class...)}), as the classes of
     * the events of type {@code E} that the calling function needs (see {@link DxfgEventClazz#eventClass()}).
     */
    @SuppressWarnings("unchecked")
    public static <E extends EventType<?>> Class<? extends E>[] toArray(final List<Class<? extends EventType<?>>> classes) {
        return (Class<? extends E>[]) classes.toArray(new Class<?>[0]);
    }

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
