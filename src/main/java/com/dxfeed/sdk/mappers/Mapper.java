// Copyright (c) 2025 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

package com.dxfeed.sdk.mappers;

import org.graalvm.nativeimage.UnmanagedMemory;
import org.graalvm.word.PointerBase;

public abstract class Mapper<JavaObjectType, NativeObjectType extends PointerBase> {

    public abstract NativeObjectType toNative(final JavaObjectType javaObject);

    public void release(final NativeObjectType nativeObject) {
        if (nativeObject.isNull()) {
            return;
        }

        cleanNative(nativeObject);
        UnmanagedMemory.free(nativeObject);
    }

    public abstract void fillNative(final JavaObjectType javaObject, final NativeObjectType nativeObject,
            boolean clean);

    public abstract void cleanNative(final NativeObjectType nativeObject);

    public final JavaObjectType toJava(final NativeObjectType nativeObject) {
        if (nativeObject.isNull()) {
            return null;
        }

        return doToJava(nativeObject);
    }

    protected abstract JavaObjectType doToJava(final NativeObjectType nativeObject);

    public abstract void fillJava(final NativeObjectType nativeObject, final JavaObjectType javaObject);
}
