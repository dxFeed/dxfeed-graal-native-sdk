// Copyright (c) 2025 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

package com.dxfeed.sdk.mappers;

import org.graalvm.nativeimage.UnmanagedMemory;
import org.graalvm.word.PointerBase;

public abstract class Mapper<V, T extends PointerBase> {

    public abstract T toNative(final V javaObject);

    public void release(final T nativeObject) {
        if (nativeObject.isNull()) {
            return;
        }

        cleanNative(nativeObject);
        UnmanagedMemory.free(nativeObject);
    }

    public abstract void fillNative(final V javaObject, final T nativeObject, boolean clean);

    public abstract void cleanNative(final T nativeObject);

    public final V toJava(final T nativeObject) {
        if (nativeObject.isNull()) {
            return null;
        }

        return doToJava(nativeObject);
    }

    protected abstract V doToJava(final T nativeObject);

    public abstract void fillJava(final T nativeObject, final V javaObject);
}
