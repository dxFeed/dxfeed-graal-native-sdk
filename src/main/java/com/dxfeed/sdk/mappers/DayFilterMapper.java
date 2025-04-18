// Copyright (c) 2025 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

package com.dxfeed.sdk.mappers;

import com.dxfeed.schedule.DayFilter;
import com.dxfeed.sdk.schedule.DxfgDayFilter;
import org.graalvm.nativeimage.c.struct.SizeOf;

public class DayFilterMapper extends JavaObjectHandlerMapper<DayFilter, DxfgDayFilter> {

    @Override
    protected int getSizeJavaObjectHandler() {
        return SizeOf.get(DxfgDayFilter.class);
    }
}
