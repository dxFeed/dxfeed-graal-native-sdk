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

    public NativeObjectType toNativeObjectWithCast(final EventType<?> javaEvent) {
        return toNative((JavaObjectType) javaEvent);
    }

    public JavaObjectType toJavaObjectWithCast(final DxfgEventType nativeEvent) {
        return toJava((NativeObjectType) nativeEvent);
    }

    public void fillNativeObjectWithCast(final EventType<?> javaEvent, final DxfgEventType nativeEvent) {
        fillNative((JavaObjectType) javaEvent, (NativeObjectType) nativeEvent, true);
    }

    public void cleanNativeObjectWithCast(final DxfgEventType nativeEvent) {
        cleanNative((NativeObjectType) nativeEvent);
    }

    public void fillJavaObjectWithCast(final DxfgEventType nativeEvent, final EventType<?> javaEvent) {
        fillJava((NativeObjectType) nativeEvent, (JavaObjectType) javaEvent);
    }

    public void releaseWithCast(final DxfgEventType nativeEvent) {
        release((NativeObjectType) nativeEvent);
    }

    public abstract NativeObjectType createNativeObject(final String symbol);

    public abstract DxfgEventClazz getEventClazz();
}
