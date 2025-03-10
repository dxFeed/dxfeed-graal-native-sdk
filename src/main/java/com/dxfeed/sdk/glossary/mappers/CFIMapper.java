// Copyright (c) 2025 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

package com.dxfeed.sdk.glossary.mappers;

import com.dxfeed.glossary.CFI;
import com.dxfeed.sdk.glossary.DxfgCFIHandle;
import com.dxfeed.sdk.mappers.JavaObjectHandlerMapper;
import org.graalvm.nativeimage.c.struct.SizeOf;

public class CFIMapper extends
        JavaObjectHandlerMapper<CFI, DxfgCFIHandle> {

    @Override
    protected int getSizeJavaObjectHandler() {
        return SizeOf.get(DxfgCFIHandle.class);
    }
}
