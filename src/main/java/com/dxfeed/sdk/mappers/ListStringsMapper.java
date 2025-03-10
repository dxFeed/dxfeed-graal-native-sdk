// Copyright (c) 2025 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

package com.dxfeed.sdk.mappers;

import com.dxfeed.sdk.javac.DxfgCStringListPointer;
import com.dxfeed.sdk.javac.DxfgCharPointerPointer;
import org.graalvm.nativeimage.c.struct.SizeOf;
import org.graalvm.nativeimage.c.type.CCharPointer;

public class ListStringsMapper
        extends ListMapper<String, CCharPointer, DxfgCharPointerPointer, DxfgCStringListPointer> {

    private final Mapper<String, CCharPointer> stringMapper;

    public ListStringsMapper(final Mapper<String, CCharPointer> stringMapper) {
        this.stringMapper = stringMapper;
    }

    @Override
    protected String toJava(final CCharPointer nativeObject) {
        return this.stringMapper.toJava(nativeObject);
    }

    @Override
    protected CCharPointer toNative(final String javaObject) {
        return this.stringMapper.toNative(javaObject);
    }

    @Override
    protected void releaseNative(final CCharPointer nativeObject) {
        this.stringMapper.release(nativeObject);
    }

    @Override
    protected int getNativeListSize() {
        return SizeOf.get(DxfgCStringListPointer.class);
    }
}
