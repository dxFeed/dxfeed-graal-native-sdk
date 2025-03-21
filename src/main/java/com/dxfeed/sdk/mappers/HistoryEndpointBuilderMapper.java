// Copyright (c) 2025 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

package com.dxfeed.sdk.mappers;

import com.dxfeed.sdk.candlewebservice.DxfgHistoryEndpointBuilderHandle;
import com.dxfeed.sdk.candlewebservice.HistoryEndpoint;
import org.graalvm.nativeimage.c.struct.SizeOf;

public class HistoryEndpointBuilderMapper extends
        JavaObjectHandlerMapper<HistoryEndpoint.Builder, DxfgHistoryEndpointBuilderHandle> {

    @Override
    protected int getSizeJavaObjectHandler() {
        return SizeOf.get(DxfgHistoryEndpointBuilderHandle.class);
    }
}
