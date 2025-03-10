// Copyright (c) 2025 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

package com.dxfeed.sdk.mappers;

import com.dxfeed.ipf.InstrumentProfile;
import com.dxfeed.sdk.ipf.DxfgInstrumentProfile;
import com.dxfeed.sdk.ipf.DxfgInstrumentProfileList;
import com.dxfeed.sdk.ipf.DxfgInstrumentProfilePointer;
import org.graalvm.nativeimage.c.struct.SizeOf;

public class ListInstrumentProfileMapper
        extends
        ListMapper<InstrumentProfile, DxfgInstrumentProfile, DxfgInstrumentProfilePointer, DxfgInstrumentProfileList> {

    private final Mapper<InstrumentProfile, DxfgInstrumentProfile> mapper;

    public ListInstrumentProfileMapper(
            final Mapper<InstrumentProfile, DxfgInstrumentProfile> mapper) {
        this.mapper = mapper;
    }

    @Override
    protected InstrumentProfile toJava(final DxfgInstrumentProfile nativeObject) {
        return this.mapper.toJava(nativeObject);
    }

    @Override
    protected DxfgInstrumentProfile toNative(final InstrumentProfile javaObject) {
        return this.mapper.toNative(javaObject);
    }

    @Override
    protected void releaseNative(final DxfgInstrumentProfile nativeObject) {
        this.mapper.release(nativeObject);
    }

    @Override
    protected int getNativeListSize() {
        return SizeOf.get(DxfgInstrumentProfileList.class);
    }
}
