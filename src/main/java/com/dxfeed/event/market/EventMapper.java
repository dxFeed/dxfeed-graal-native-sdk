// Copyright (c) 2025 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

package com.dxfeed.event.market;

import com.dxfeed.event.EventType;
import com.dxfeed.sdk.events.DxfgEventType;
import com.dxfeed.sdk.mappers.Mapper;
import org.graalvm.word.WordFactory;

public abstract class EventMapper<V extends EventType<?>, T extends DxfgEventType>
        extends Mapper<V, T> {

    public T toNative(final V javaObject) {
        if (javaObject == null) {
            return WordFactory.nullPointer();
        }

        final T nativeObject = createNativeObject();

        fillNative(javaObject, nativeObject, true);

        return nativeObject;
    }

    protected abstract T createNativeObject();

    public T toNativeObjectWithCast(final EventType<?> javaEvent) {
        return toNative((V) javaEvent);
    }

    public V toJavaObjectWithCast(final DxfgEventType nativeEvent) {
        return toJava((T) nativeEvent);
    }

    public void fillNativeObjectWithCast(final EventType<?> javaEvent, final DxfgEventType nativeEvent) {
        fillNative((V) javaEvent, (T) nativeEvent, true);
    }

    public void cleanNativeObjectWithCast(final DxfgEventType nativeEvent) {
        cleanNative((T) nativeEvent);
    }

    public void fillJavaObjectWithCast(final DxfgEventType nativeEvent, final EventType<?> javaEvent) {
        fillJava((T) nativeEvent, (V) javaEvent);
    }

    public void releaseWithCast(final DxfgEventType nativeEvent) {
        release((T) nativeEvent);
    }

    public abstract T createNativeObject(final String symbol);
}
