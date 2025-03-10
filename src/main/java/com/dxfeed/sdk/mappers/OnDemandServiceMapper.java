// Copyright (c) 2025 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

package com.dxfeed.sdk.mappers;

import com.dxfeed.ondemand.OnDemandService;
import com.dxfeed.sdk.ondemand.DxfgOnDemandService;
import org.graalvm.nativeimage.c.struct.SizeOf;

public class OnDemandServiceMapper extends JavaObjectHandlerMapper<OnDemandService, DxfgOnDemandService> {

    @Override
    protected int getSizeJavaObjectHandler() {
        return SizeOf.get(DxfgOnDemandService.class);
    }
}
