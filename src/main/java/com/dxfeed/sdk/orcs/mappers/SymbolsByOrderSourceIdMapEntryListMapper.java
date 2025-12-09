// Copyright (c) 2025 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

package com.dxfeed.sdk.orcs.mappers;

import com.dxfeed.sdk.mappers.ListMapper;
import com.dxfeed.sdk.orcs.DxfgSymbolsByOrderSourceIdMapEntryListPointer;
import com.dxfeed.sdk.orcs.DxfgSymbolsByOrderSourceIdMapEntryPointer;
import com.dxfeed.sdk.orcs.DxfgSymbolsByOrderSourceIdMapEntryPointerPointer;
import java.util.Map;
import java.util.Set;
import org.graalvm.nativeimage.c.struct.SizeOf;

public class SymbolsByOrderSourceIdMapEntryListMapper extends
        ListMapper<Map.Entry<Integer, Set<String>>, DxfgSymbolsByOrderSourceIdMapEntryPointer, DxfgSymbolsByOrderSourceIdMapEntryPointerPointer,
                DxfgSymbolsByOrderSourceIdMapEntryListPointer> {

    public SymbolsByOrderSourceIdMapEntryListMapper() {
    }

    @Override
    protected Map.Entry<Integer, Set<String>> toJava(
            final DxfgSymbolsByOrderSourceIdMapEntryPointer nativeObject) {
        return Mappers.SYMBOLS_BY_ORDER_SOURCE_ID_MAP_ENTRY_MAPPER.toJava(nativeObject);
    }

    @Override
    protected DxfgSymbolsByOrderSourceIdMapEntryPointer toNative(final Map.Entry<Integer, Set<String>> javaObject) {
        return Mappers.SYMBOLS_BY_ORDER_SOURCE_ID_MAP_ENTRY_MAPPER.toNative(javaObject);
    }

    @Override
    protected void releaseNative(final DxfgSymbolsByOrderSourceIdMapEntryPointer nativeObject) {
        Mappers.SYMBOLS_BY_ORDER_SOURCE_ID_MAP_ENTRY_MAPPER.release(nativeObject);
    }

    @Override
    protected int getNativeListSize() {
        return SizeOf.get(DxfgSymbolsByOrderSourceIdMapEntryListPointer.class);
    }
}
