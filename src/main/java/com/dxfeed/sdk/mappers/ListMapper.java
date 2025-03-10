// Copyright (c) 2025 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

package com.dxfeed.sdk.mappers;

import com.dxfeed.sdk.javac.CList;
import com.dxfeed.sdk.javac.CPointerPointer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import org.graalvm.nativeimage.UnmanagedMemory;
import org.graalvm.word.PointerBase;
import org.graalvm.word.WordFactory;

public abstract class ListMapper<
        JavaObject,
        NativeObject extends PointerBase,
        NativeObjectPointer extends CPointerPointer<NativeObject>,
        NativeList extends CList<NativeObjectPointer>
        > {

    //https://github.com/graalvm/graal-jvmci-8/blob/master/jvmci/jdk.vm.ci.code/src/jdk/vm/ci/code/TargetDescription.java
    //ConfigurationValues.getTarget().arch.getWordSize() or ConfigurationValues.getTarget().wordSize;
    protected static final int SIZE_OF_C_POINTER = 8;

    public NativeList toNativeList(final Collection<? extends JavaObject> javaCollection) {
        final NativeList nativeList = UnmanagedMemory.calloc(getNativeListSize());

        if (javaCollection == null || javaCollection.isEmpty()) {
            nativeList.setSize(0);
            nativeList.setElements(WordFactory.nullPointer());
        } else {
            final NativeObjectPointer nativeListElements = UnmanagedMemory.calloc(
                    SIZE_OF_C_POINTER * javaCollection.size());
            int i = 0;

            for (final JavaObject javaObject : javaCollection) {
                nativeListElements.addressOf(i++).write(toNative(javaObject));
            }

            nativeList.setSize(javaCollection.size());
            nativeList.setElements(nativeListElements);
        }

        return nativeList;
    }

    public List<JavaObject> toJavaList(final NativeList nativeList) {
        if (nativeList.isNull() || nativeList.getSize() == 0) {
            return Collections.emptyList();
        }

        final List<JavaObject> javaList = new ArrayList<>(nativeList.getSize());

        for (int i = 0; i < nativeList.getSize(); i++) {
            javaList.add(toJava(nativeList.getElements().addressOf(i).read()));
        }

        return javaList;
    }

    public void release(final NativeList nativeList) {
        if (nativeList.isNull()) {
            return;
        }

        if (nativeList.getElements().isNonNull()) {
            for (int i = 0; i < nativeList.getSize(); ++i) {
                final NativeObject nativeObject = nativeList.getElements().addressOf(i).read();

                if (nativeObject.isNonNull()) {
                    releaseNative(nativeObject);
                }
            }

            UnmanagedMemory.free(nativeList.getElements());
        }

        UnmanagedMemory.free(nativeList);
    }

    protected abstract JavaObject toJava(final NativeObject nativeObject);

    protected abstract NativeObject toNative(final JavaObject javaObject);

    protected abstract void releaseNative(final NativeObject nativeObject);

    protected abstract int getNativeListSize();
}
