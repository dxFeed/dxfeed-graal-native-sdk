// Copyright (c) 2025 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

package com.dxfeed.event;

import com.dxfeed.sdk.events.DxfgEventClazz;
import com.dxfeed.sdk.events.DxfgEventType;
import com.dxfeed.sdk.mappers.Mapper;
import org.graalvm.word.WordFactory;

public abstract class EventMapper<JavaObjectType extends EventType<?>, NativeObjectType extends DxfgEventType>
        extends Mapper<JavaObjectType, NativeObjectType> {

    public NativeObjectType toNative(final JavaObjectType javaObject) {
        if (javaObject == null) {
            return WordFactory.nullPointer();
        }

        final NativeObjectType nativeObject = createNativeObject();

        fillNative(javaObject, nativeObject, true);

        return nativeObject;
    }

    protected abstract NativeObjectType createNativeObject();

    // EventMappers chooses the mapper by the class of the event, so the events and the native structures passed to the
    // methods "...WithCast" are of the types of this mapper.

    @SuppressWarnings("unchecked")
    public NativeObjectType toNativeObjectWithCast(final EventType<?> javaEvent) {
        return toNative((JavaObjectType) javaEvent);
    }

    @SuppressWarnings("unchecked")
    public JavaObjectType toJavaObjectWithCast(final DxfgEventType nativeEvent) {
        return toJava((NativeObjectType) nativeEvent);
    }

    @SuppressWarnings("unchecked")
    public void fillNativeObjectWithCast(final EventType<?> javaEvent, final DxfgEventType nativeEvent) {
        fillNative((JavaObjectType) javaEvent, (NativeObjectType) nativeEvent, true);
    }

    @SuppressWarnings("unchecked")
    public void cleanNativeObjectWithCast(final DxfgEventType nativeEvent) {
        cleanNative((NativeObjectType) nativeEvent);
    }

    @SuppressWarnings("unchecked")
    public void fillJavaObjectWithCast(final DxfgEventType nativeEvent, final EventType<?> javaEvent) {
        fillJava((NativeObjectType) nativeEvent, (JavaObjectType) javaEvent);
    }

    @SuppressWarnings("unchecked")
    public void releaseWithCast(final DxfgEventType nativeEvent) {
        release((NativeObjectType) nativeEvent);
    }

    public abstract NativeObjectType createNativeObject(final String symbol);

    public abstract DxfgEventClazz getEventClazz();
}
