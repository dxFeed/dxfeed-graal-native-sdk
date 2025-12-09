// Copyright (c) 2025 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

package com.dxfeed.sdk.orcs.mappers;

import com.dxfeed.sdk.NativeUtils;
import com.dxfeed.sdk.mappers.Mapper;
import com.dxfeed.sdk.orcs.DxfgSymbolsByOrderSourceIdMapEntryPointer;
import java.util.AbstractMap;
import java.util.Map;
import java.util.Set;
import org.graalvm.nativeimage.UnmanagedMemory;
import org.graalvm.nativeimage.c.struct.SizeOf;
import org.graalvm.word.WordFactory;

public class SymbolsByOrderSourceIdMapEntryMapper extends
        Mapper<Map.Entry<Integer, Set<String>>, DxfgSymbolsByOrderSourceIdMapEntryPointer> {

    public SymbolsByOrderSourceIdMapEntryMapper() {
    }

    @Override
    public DxfgSymbolsByOrderSourceIdMapEntryPointer toNative(final Map.Entry<Integer, Set<String>> javaObject) {
        if (javaObject == null) {
            return WordFactory.nullPointer();
        }

        final DxfgSymbolsByOrderSourceIdMapEntryPointer nativeObject = UnmanagedMemory.calloc(
                SizeOf.get(DxfgSymbolsByOrderSourceIdMapEntryPointer.class));

        fillNative(javaObject, nativeObject, false);

        return nativeObject;
    }

    @Override
    public void fillNative(final Map.Entry<Integer, Set<String>> javaObject,
            final DxfgSymbolsByOrderSourceIdMapEntryPointer nativeObject, boolean clean) {
        if (clean) {
            cleanNative(nativeObject);
        }

        nativeObject.setOrderSourceId(javaObject.getKey());
        nativeObject.setSymbols(NativeUtils.MAPPER_STRINGS.toNativeList(javaObject.getValue()));
    }

    @Override
    public void cleanNative(final DxfgSymbolsByOrderSourceIdMapEntryPointer nativeObject) {
        if (nativeObject.isNull()) {
            return;
        }

        NativeUtils.MAPPER_STRINGS.release(nativeObject.getSymbols());
    }

    @Override
    protected Map.Entry<Integer, Set<String>> doToJava(final DxfgSymbolsByOrderSourceIdMapEntryPointer nativeObject) {
        return new AbstractMap.SimpleEntry<>(
                nativeObject.getOrderSourceId(),
                Set.copyOf(NativeUtils.MAPPER_STRINGS.toJavaList(nativeObject.getSymbols()))
        );
    }

    @Override
    public void fillJava(final DxfgSymbolsByOrderSourceIdMapEntryPointer nativeObject,
            final Map.Entry<Integer, Set<String>> javaObject) {
    }
}
