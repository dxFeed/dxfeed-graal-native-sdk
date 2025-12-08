// Copyright (c) 2025 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

package com.dxfeed.sdk.orcs.mappers;

import com.dxfeed.sdk.mappers.JavaObjectHandlerMapper;
import com.dxfeed.sdk.orcs.DxfgPriceLevelServiceHandle;
import com.dxfeed.sdk.orcs.PriceLevelServiceHolder;
import org.graalvm.nativeimage.c.struct.SizeOf;

public class PriceLevelServiceMapper extends
        JavaObjectHandlerMapper<PriceLevelServiceHolder, DxfgPriceLevelServiceHandle> {

    @Override
    protected int getSizeJavaObjectHandler() {
        return SizeOf.get(DxfgPriceLevelServiceHandle.class);
    }
}
