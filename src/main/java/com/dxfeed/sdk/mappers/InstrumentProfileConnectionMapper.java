// Copyright (c) 2025 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

package com.dxfeed.sdk.mappers;

import com.dxfeed.ipf.live.InstrumentProfileConnection;
import com.dxfeed.sdk.ipf.DxfgInstrumentProfileConnection;
import org.graalvm.nativeimage.c.struct.SizeOf;

public class InstrumentProfileConnectionMapper
        extends JavaObjectHandlerMapper<InstrumentProfileConnection, DxfgInstrumentProfileConnection> {

    @Override
    protected int getSizeJavaObjectHandler() {
        return SizeOf.get(DxfgInstrumentProfileConnection.class);
    }
}
