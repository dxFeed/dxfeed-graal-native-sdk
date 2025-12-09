// Copyright (c) 2025 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

package com.dxfeed.sdk.orcs.mappers;

import com.dxfeed.event.IndexedEventSource;
import com.dxfeed.sdk.NativeUtils;
import com.dxfeed.sdk.mappers.Mapper;
import com.dxfeed.sdk.orcs.DxfgSymbolsByOrderSourceMapEntryPointer;
import java.util.AbstractMap;
import java.util.Map;
import java.util.Set;
import org.graalvm.nativeimage.UnmanagedMemory;
import org.graalvm.nativeimage.c.struct.SizeOf;
import org.graalvm.word.WordFactory;

public class SymbolsByOrderSourceMapEntryMapper extends
        Mapper<Map.Entry<? extends IndexedEventSource, Set<String>>, DxfgSymbolsByOrderSourceMapEntryPointer> {


    public SymbolsByOrderSourceMapEntryMapper() {
    }

    @Override
    public DxfgSymbolsByOrderSourceMapEntryPointer toNative(
            final Map.Entry<? extends IndexedEventSource, Set<String>> javaObject) {
        if (javaObject == null) {
            return WordFactory.nullPointer();
        }

        final DxfgSymbolsByOrderSourceMapEntryPointer nativeObject = UnmanagedMemory.calloc(
                SizeOf.get(DxfgSymbolsByOrderSourceMapEntryPointer.class));

        fillNative(javaObject, nativeObject, false);

        return nativeObject;
    }

    @Override
    public void fillNative(final Map.Entry<? extends IndexedEventSource, Set<String>> javaObject,
            final DxfgSymbolsByOrderSourceMapEntryPointer nativeObject, boolean clean) {
        if (clean) {
            cleanNative(nativeObject);
        }

        nativeObject.setOrderSource(NativeUtils.MAPPER_INDEXED_EVENT_SOURCE.toNative(javaObject.getKey()));
        nativeObject.setSymbols(NativeUtils.MAPPER_STRINGS.toNativeList(javaObject.getValue()));
    }

    @Override
    public void cleanNative(final DxfgSymbolsByOrderSourceMapEntryPointer nativeObject) {
        if (nativeObject.isNull()) {
            return;
        }

        NativeUtils.MAPPER_INDEXED_EVENT_SOURCE.release(nativeObject.getOrderSource());
        NativeUtils.MAPPER_STRINGS.release(nativeObject.getSymbols());
    }

    @Override
    protected Map.Entry<? extends IndexedEventSource, Set<String>> doToJava(
            final DxfgSymbolsByOrderSourceMapEntryPointer nativeObject) {
        return new AbstractMap.SimpleEntry<>(
                NativeUtils.MAPPER_INDEXED_EVENT_SOURCE.toJava(nativeObject.getOrderSource()),
                Set.copyOf((NativeUtils.MAPPER_STRINGS.toJavaList(nativeObject.getSymbols()))));
    }

    @Override
    public void fillJava(final DxfgSymbolsByOrderSourceMapEntryPointer nativeObject,
            final Map.Entry<? extends IndexedEventSource, Set<String>> javaObject) {
    }
}
