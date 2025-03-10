// Copyright (c) 2025 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

package com.dxfeed.sdk.mappers;

import com.dxfeed.ipf.InstrumentProfile;
import com.dxfeed.sdk.ipf.DxfgInstrumentProfile;
import org.graalvm.nativeimage.c.struct.SizeOf;

public class InstrumentProfileMapper
        extends JavaObjectHandlerMapper<InstrumentProfile, DxfgInstrumentProfile> {

    @Override
    protected int getSizeJavaObjectHandler() {
        return SizeOf.get(DxfgInstrumentProfile.class);
    }
}
