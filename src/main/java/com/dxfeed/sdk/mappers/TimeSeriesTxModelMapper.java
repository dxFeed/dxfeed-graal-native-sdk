// Copyright (c) 2026 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

package com.dxfeed.sdk.mappers;

import com.dxfeed.sdk.model.DxfgTimeSeriesTxModelHandle;
import com.dxfeed.sdk.model.tx.TimeSeriesTxModel;
import org.graalvm.nativeimage.c.struct.SizeOf;

public class TimeSeriesTxModelMapper
        extends JavaObjectHandlerMapper<TimeSeriesTxModel<?>, DxfgTimeSeriesTxModelHandle> {

    @Override
    protected int getSizeJavaObjectHandler() {
        return SizeOf.get(DxfgTimeSeriesTxModelHandle.class);
    }
}
