// Copyright (c) 2025 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

package com.dxfeed.sdk.mappers;

import com.dxfeed.model.TimeSeriesEventModel;
import com.dxfeed.sdk.model.DxfgTimeSeriesEventModel;
import org.graalvm.nativeimage.c.struct.SizeOf;

public class TimeSeriesEventModelMapper
        extends JavaObjectHandlerMapper<TimeSeriesEventModel<?>, DxfgTimeSeriesEventModel> {

    @Override
    protected int getSizeJavaObjectHandler() {
        return SizeOf.get(DxfgTimeSeriesEventModel.class);
    }
}
