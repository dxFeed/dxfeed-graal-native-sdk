// Copyright (c) 2025 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

package com.dxfeed.sdk.mappers;

import com.dxfeed.sdk.javac.JavaObjectHandler;
import com.dxfeed.sdk.javac.NativeListener;
import org.graalvm.nativeimage.ObjectHandle;
import org.graalvm.nativeimage.ObjectHandles;
import org.graalvm.nativeimage.UnmanagedMemory;
import org.graalvm.nativeimage.c.struct.SizeOf;
import org.graalvm.word.WordFactory;

public class JavaObjectHandlerMapper<JavaObjectType, NativeObjectType extends JavaObjectHandler<JavaObjectType>> extends
        Mapper<JavaObjectType, NativeObjectType> {

    @Override
    public NativeObjectType toNative(final JavaObjectType javaObject) {
        if (javaObject == null) {
            return WordFactory.nullPointer();
        }

        final NativeObjectType nativeObject = UnmanagedMemory.calloc(getSizeJavaObjectHandler());
        fillNative(javaObject, nativeObject,
                false); //There is no need to destroy the object handle since the memory has just been allocated and zeroed.

        return nativeObject;
    }

    public NativeObjectType toNativeArray(final JavaObjectType[] javaObjects) {
        if (javaObjects == null || javaObjects.length == 0) {
            return WordFactory.nullPointer();
        }

        final NativeObjectType nativeObject = UnmanagedMemory.calloc(getSizeJavaObjectHandler() * javaObjects.length);

        for (int i = 0; i < javaObjects.length; i++) {
            // addressOf returns the pointer type of the structure, which is NativeObjectType.
            @SuppressWarnings("unchecked") final NativeObjectType element = (NativeObjectType) nativeObject.addressOf(i);
            fillNative(javaObjects[i], element, false);
        }

        return nativeObject;
    }

    public void releaseNativeArray(final NativeObjectType nativeArray, int size) {
        if (nativeArray.isNonNull() && size > 0) {
            for (int i = 0; i < size; i++) {
                @SuppressWarnings("unchecked") final NativeObjectType element = (NativeObjectType) nativeArray.addressOf(i);
                cleanNative(element);
            }

            UnmanagedMemory.free(nativeArray);
        }
    }

    @Override
    public final void fillNative(final JavaObjectType javaObject, final NativeObjectType nativeObject, boolean clean) {
        if (clean) {
            cleanNative(nativeObject);
        }

        nativeObject.setJavaObjectHandler(ObjectHandles.getGlobal().create(javaObject));
        if (javaObject instanceof NativeListener) {
            ((NativeListener) javaObject).handleCreated();
        }
    }

    @Override
    public final void cleanNative(final NativeObjectType nativeObject) {
        final ObjectHandle handle = nativeObject.getJavaObjectHandler();
        final Object javaObject = ObjectHandles.getGlobal().get(handle);
        ObjectHandles.getGlobal().destroy(handle);
        // The last released handle deactivates the listener, so that it does not call the native function anymore.
        if (javaObject instanceof NativeListener) {
            ((NativeListener) javaObject).handleReleased();
        }
    }

    @Override
    protected JavaObjectType doToJava(final NativeObjectType nativeObject) {
        return ObjectHandles.getGlobal().get(nativeObject.getJavaObjectHandler());
    }

    @Override
    public void fillJava(final NativeObjectType nativeObject, final JavaObjectType javaObject) {
        throw new IllegalStateException("The Java object does not support setters.");
    }

    protected int getSizeJavaObjectHandler() {
        return SizeOf.get(JavaObjectHandler.class);
    }

}
