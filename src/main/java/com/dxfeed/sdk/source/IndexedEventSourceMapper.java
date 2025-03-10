// Copyright (c) 2025 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

package com.dxfeed.sdk.source;

import com.dxfeed.event.IndexedEventSource;
import com.dxfeed.event.market.OrderSource;
import com.dxfeed.sdk.mappers.Mapper;
import org.graalvm.nativeimage.UnmanagedMemory;
import org.graalvm.nativeimage.c.struct.SizeOf;
import org.graalvm.nativeimage.c.type.CCharPointer;
import org.graalvm.word.WordFactory;

public class IndexedEventSourceMapper extends Mapper<IndexedEventSource, DxfgIndexedEventSource> {

    private final Mapper<String, CCharPointer> stringMapper;

    public IndexedEventSourceMapper(final Mapper<String, CCharPointer> stringMapper) {
        this.stringMapper = stringMapper;
    }

    @Override
    public DxfgIndexedEventSource toNative(final IndexedEventSource javaObject) {
        if (javaObject == null) {
            return WordFactory.nullPointer();
        }

        final DxfgIndexedEventSource nativeObject = UnmanagedMemory.calloc(
                SizeOf.get(DxfgIndexedEventSource.class)
        );

        fillNative(javaObject, nativeObject, false);

        return nativeObject;
    }

    @Override
    public void fillNative(final IndexedEventSource javaObject, final DxfgIndexedEventSource nativeObject,
            boolean clean) {
        if (clean) {
            cleanNative(nativeObject);
        }

        if (javaObject instanceof OrderSource) {
            nativeObject.setType(DxfgIndexedEventSourceType.ORDER_SOURCE.getCValue());
        } else {
            nativeObject.setType(DxfgIndexedEventSourceType.INDEXED_EVENT_SOURCE.getCValue());
        }
        nativeObject.setId(javaObject.id());
        nativeObject.setName(this.stringMapper.toNative(javaObject.name()));
    }

    @Override
    public void cleanNative(final DxfgIndexedEventSource nativeObject) {
        this.stringMapper.release(nativeObject.getName());
    }

    @Override
    protected IndexedEventSource doToJava(final DxfgIndexedEventSource nativeObject) {
        switch (DxfgIndexedEventSourceType.fromCValue(nativeObject.getType())) {
            case INDEXED_EVENT_SOURCE:
                return new IndexedEventSource(
                        nativeObject.getId(),
                        this.stringMapper.toJava(nativeObject.getName())
                );
            case ORDER_SOURCE:
                if (nativeObject.getName().isNonNull()) {
                    return OrderSource.valueOf(this.stringMapper.toJava(nativeObject.getName()));
                } else {
                    return OrderSource.valueOf(nativeObject.getId());
                }
            default:
                throw new IllegalStateException();
        }
    }

    @Override
    public void fillJava(final DxfgIndexedEventSource nativeObject, final IndexedEventSource javaObject) {
        throw new IllegalStateException("The Java object does not support setters.");
    }
}
