// Copyright (c) 2026 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

package com.dxfeed.sdk.glossary.mappers;

import com.dxfeed.glossary.CFI;
import com.dxfeed.sdk.glossary.DxfgCFIValueHandle;
import com.dxfeed.sdk.glossary.DxfgCFIValueHandlePointer;
import com.dxfeed.sdk.glossary.DxfgCFIValueList;
import com.dxfeed.sdk.mappers.ListMapper;
import com.dxfeed.sdk.mappers.Mapper;
import org.graalvm.nativeimage.c.struct.SizeOf;

/**
 * Maps the CFI values to a {@code dxfg_v4_cfi_value_list}: every element is a handle of its own.
 */
public class ListCFIValueMapper
        extends ListMapper<CFI.Value, DxfgCFIValueHandle, DxfgCFIValueHandlePointer, DxfgCFIValueList> {

    private final Mapper<CFI.Value, DxfgCFIValueHandle> mapper;

    public ListCFIValueMapper(final Mapper<CFI.Value, DxfgCFIValueHandle> mapper) {
        this.mapper = mapper;
    }

    @Override
    protected CFI.Value toJava(final DxfgCFIValueHandle nativeObject) {
        return this.mapper.toJava(nativeObject);
    }

    @Override
    protected DxfgCFIValueHandle toNative(final CFI.Value javaObject) {
        return this.mapper.toNative(javaObject);
    }

    @Override
    protected void releaseNative(final DxfgCFIValueHandle nativeObject) {
        this.mapper.release(nativeObject);
    }

    @Override
    protected int getNativeListSize() {
        return SizeOf.get(DxfgCFIValueList.class);
    }
}
