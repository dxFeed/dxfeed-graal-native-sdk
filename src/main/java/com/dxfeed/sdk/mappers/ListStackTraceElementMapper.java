// Copyright (c) 2025 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

package com.dxfeed.sdk.mappers;

import com.dxfeed.sdk.exception.DxfgStackTraceElement;
import com.dxfeed.sdk.exception.DxfgStackTraceElementList;
import com.dxfeed.sdk.exception.DxfgStackTraceElementPointer;
import org.graalvm.nativeimage.c.struct.SizeOf;

public class ListStackTraceElementMapper
        extends
        ListMapper<StackTraceElement, DxfgStackTraceElement, DxfgStackTraceElementPointer, DxfgStackTraceElementList> {

    private final Mapper<StackTraceElement, DxfgStackTraceElement> mapper;

    public ListStackTraceElementMapper(final Mapper<StackTraceElement, DxfgStackTraceElement> mapper) {
        this.mapper = mapper;
    }

    @Override
    protected StackTraceElement toJava(final DxfgStackTraceElement nativeObject) {
        return this.mapper.toJava(nativeObject);
    }

    @Override
    protected DxfgStackTraceElement toNative(final StackTraceElement javaObject) {
        return this.mapper.toNative(javaObject);
    }

    @Override
    protected void releaseNative(final DxfgStackTraceElement nativeObject) {
        this.mapper.release(nativeObject);
    }

    @Override
    protected int getNativeListSize() {
        return SizeOf.get(DxfgStackTraceElementList.class);
    }
}
