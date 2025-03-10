// Copyright (c) 2025 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

package com.dxfeed.sdk.symbol;

import com.dxfeed.sdk.mappers.ListMapper;
import com.dxfeed.sdk.mappers.Mapper;
import org.graalvm.nativeimage.UnmanagedMemory;
import org.graalvm.nativeimage.c.struct.SizeOf;

public class ListSymbolMapper extends
        ListMapper<Object, DxfgSymbol, DxfgSymbolPointer, DxfgSymbolList> {

    private final Mapper<Object, DxfgSymbol> symbolMapper;

    public ListSymbolMapper(final Mapper<Object, DxfgSymbol> symbolMapper) {
        this.symbolMapper = symbolMapper;
    }

    public void release(final DxfgSymbolList nativeList) {
        for (int i = 0; i < nativeList.getSize(); ++i) {
            this.symbolMapper.release(nativeList.getElements().addressOf(i).read());
        }
        UnmanagedMemory.free(nativeList.getElements());
        UnmanagedMemory.free(nativeList);
    }

    @Override
    protected Object toJava(final DxfgSymbol nativeObject) {
        return this.symbolMapper.toJava(nativeObject);
    }

    @Override
    protected DxfgSymbol toNative(final Object javaObject) {
        return this.symbolMapper.toNative(javaObject);
    }

    @Override
    protected void releaseNative(final DxfgSymbol nativeObject) {
        this.symbolMapper.release(nativeObject);
    }

    @Override
    protected int getNativeListSize() {
        return SizeOf.get(DxfgSymbolList.class);
    }
}
