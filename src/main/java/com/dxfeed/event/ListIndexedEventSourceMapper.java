// Copyright (c) 2025 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

package com.dxfeed.event;

import com.dxfeed.sdk.events.DxfgIndexedEventSourceList;
import com.dxfeed.sdk.mappers.ListMapper;
import com.dxfeed.sdk.mappers.Mapper;
import com.dxfeed.sdk.source.DxfgIndexedEventSourcePointer;
import com.dxfeed.sdk.source.DxfgIndexedEventSourcePointerPointer;
import org.graalvm.nativeimage.c.struct.SizeOf;

public class ListIndexedEventSourceMapper extends
        ListMapper<IndexedEventSource, DxfgIndexedEventSourcePointer, DxfgIndexedEventSourcePointerPointer, DxfgIndexedEventSourceList> {

    private final Mapper<IndexedEventSource, DxfgIndexedEventSourcePointer> sourceMappers;

    public ListIndexedEventSourceMapper(
            final Mapper<IndexedEventSource, DxfgIndexedEventSourcePointer> sourceMappers) {
        this.sourceMappers = sourceMappers;
    }

    @Override
    protected IndexedEventSource toJava(final DxfgIndexedEventSourcePointer nativeObject) {
        return sourceMappers.toJava(nativeObject);
    }

    @Override
    protected void releaseNative(final DxfgIndexedEventSourcePointer nativeObject) {
        sourceMappers.release(nativeObject);
    }

    @Override
    protected DxfgIndexedEventSourcePointer toNative(final IndexedEventSource javaObject) {
        return sourceMappers.toNative(javaObject);
    }

    @Override
    protected int getNativeListSize() {
        return SizeOf.get(DxfgIndexedEventSourceList.class);
    }
}
