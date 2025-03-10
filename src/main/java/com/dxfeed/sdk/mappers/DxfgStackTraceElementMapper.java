// Copyright (c) 2025 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

package com.dxfeed.sdk.mappers;

import com.dxfeed.sdk.exception.DxfgStackTraceElement;
import org.graalvm.nativeimage.UnmanagedMemory;
import org.graalvm.nativeimage.c.struct.SizeOf;
import org.graalvm.nativeimage.c.type.CCharPointer;
import org.graalvm.word.WordFactory;

public class DxfgStackTraceElementMapper extends Mapper<StackTraceElement, DxfgStackTraceElement> {

    protected final Mapper<String, CCharPointer> stringMapper;

    public DxfgStackTraceElementMapper(final Mapper<String, CCharPointer> stringMapper) {
        this.stringMapper = stringMapper;
    }

    @Override
    public DxfgStackTraceElement toNative(final StackTraceElement javaObject) {
        if (javaObject == null) {
            return WordFactory.nullPointer();
        }
        final DxfgStackTraceElement nativeObject = UnmanagedMemory.calloc(SizeOf.get(DxfgStackTraceElement.class));
        fillNative(javaObject, nativeObject, false);
        return nativeObject;
    }

    @Override
    public final void fillNative(final StackTraceElement javaObject, final DxfgStackTraceElement nativeObject,
            boolean clean) {
        if (clean) {
            cleanNative(nativeObject);
        }

        nativeObject.setClassName(this.stringMapper.toNative(javaObject.getClassName()));
        nativeObject.setClassLoaderName(this.stringMapper.toNative(javaObject.getClassLoaderName()));
        nativeObject.setFileName(this.stringMapper.toNative(javaObject.getFileName()));
        nativeObject.setMethodName(this.stringMapper.toNative(javaObject.getMethodName()));
        nativeObject.setLineNumber(javaObject.getLineNumber());
        nativeObject.setIsNativeMethod(javaObject.isNativeMethod() ? 1 : 0);
        nativeObject.setModuleName(this.stringMapper.toNative(javaObject.getModuleName()));
        nativeObject.setModuleVersion(this.stringMapper.toNative(javaObject.getModuleVersion()));
    }

    @Override
    public final void cleanNative(final DxfgStackTraceElement nativeObject) {
        stringMapper.release(nativeObject.getClassName());
        stringMapper.release(nativeObject.getClassLoaderName());
        stringMapper.release(nativeObject.getFileName());
        stringMapper.release(nativeObject.getModuleName());
        stringMapper.release(nativeObject.getModuleVersion());
    }

    @Override
    protected StackTraceElement doToJava(final DxfgStackTraceElement nativeObject) {
        throw new IllegalStateException();
    }

    @Override
    public void fillJava(final DxfgStackTraceElement nativeObject, final StackTraceElement javaObject) {
        throw new IllegalStateException("The Java object does not support setters.");
    }
}
