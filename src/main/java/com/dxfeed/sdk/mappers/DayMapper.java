// Copyright (c) 2025 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

package com.dxfeed.sdk.mappers;

import com.dxfeed.schedule.Day;
import com.dxfeed.sdk.schedule.DxfgDay;
import org.graalvm.nativeimage.c.struct.SizeOf;

public class DayMapper extends JavaObjectHandlerMapper<Day, DxfgDay> {

    @Override
    protected int getSizeJavaObjectHandler() {
        return SizeOf.get(DxfgDay.class);
    }
}
