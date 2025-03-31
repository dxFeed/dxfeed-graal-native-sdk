// Copyright (c) 2025 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

package com.dxfeed.event;

import com.dxfeed.sdk.events.DxfgIndexedEventSourceList;
import com.dxfeed.sdk.events.DxfgIndexedEventSourcePointer;
import com.dxfeed.sdk.mappers.ListMapper;
import com.dxfeed.sdk.mappers.Mapper;
import com.dxfeed.sdk.source.DxfgIndexedEventSource;
import org.graalvm.nativeimage.c.struct.SizeOf;

public class ListIndexedEventSourceMapper extends
        ListMapper<IndexedEventSource, DxfgIndexedEventSource, DxfgIndexedEventSourcePointer, DxfgIndexedEventSourceList> {

    private final Mapper<IndexedEventSource, DxfgIndexedEventSource> sourceMappers;

    public ListIndexedEventSourceMapper(
            final Mapper<IndexedEventSource, DxfgIndexedEventSource> sourceMappers) {
        this.sourceMappers = sourceMappers;
    }

    @Override
    protected IndexedEventSource toJava(final DxfgIndexedEventSource nativeObject) {
        return sourceMappers.toJava(nativeObject);
    }

    @Override
    protected void releaseNative(final DxfgIndexedEventSource nativeObject) {
        sourceMappers.release(nativeObject);
    }

    @Override
    protected DxfgIndexedEventSource toNative(final IndexedEventSource javaObject) {
        return sourceMappers.toNative(javaObject);
    }

    @Override
    protected int getNativeListSize() {
        return SizeOf.get(DxfgIndexedEventSourceList.class);
    }
}
