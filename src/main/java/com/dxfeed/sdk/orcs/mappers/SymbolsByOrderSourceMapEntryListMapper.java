// Copyright (c) 2025 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

package com.dxfeed.sdk.orcs.mappers;

import com.dxfeed.event.IndexedEventSource;
import com.dxfeed.sdk.mappers.ListMapper;
import com.dxfeed.sdk.orcs.DxfgSymbolsByOrderSourceMapEntryListPointer;
import com.dxfeed.sdk.orcs.DxfgSymbolsByOrderSourceMapEntryPointer;
import com.dxfeed.sdk.orcs.DxfgSymbolsByOrderSourceMapEntryPointerPointer;
import java.util.Map;
import java.util.Set;
import org.graalvm.nativeimage.c.struct.SizeOf;

public class SymbolsByOrderSourceMapEntryListMapper extends
        ListMapper<Map.Entry<? extends IndexedEventSource, Set<String>>, DxfgSymbolsByOrderSourceMapEntryPointer, DxfgSymbolsByOrderSourceMapEntryPointerPointer,
                DxfgSymbolsByOrderSourceMapEntryListPointer> {

    public SymbolsByOrderSourceMapEntryListMapper() {
    }

    @Override
    protected Map.Entry<? extends IndexedEventSource, Set<String>> toJava(
            final DxfgSymbolsByOrderSourceMapEntryPointer nativeObject) {
        return Mappers.SYMBOLS_BY_ORDER_SOURCE_MAP_ENTRY_MAPPER.toJava(nativeObject);
    }

    @Override
    protected DxfgSymbolsByOrderSourceMapEntryPointer toNative(
            final Map.Entry<? extends IndexedEventSource, Set<String>> javaObject) {
        return Mappers.SYMBOLS_BY_ORDER_SOURCE_MAP_ENTRY_MAPPER.toNative(javaObject);
    }

    @Override
    protected void releaseNative(final DxfgSymbolsByOrderSourceMapEntryPointer nativeObject) {
        Mappers.SYMBOLS_BY_ORDER_SOURCE_MAP_ENTRY_MAPPER.release(nativeObject);
    }

    @Override
    protected int getNativeListSize() {
        return SizeOf.get(DxfgSymbolsByOrderSourceMapEntryListPointer.class);
    }
}
