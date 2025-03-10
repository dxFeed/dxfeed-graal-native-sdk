// Copyright (c) 2025 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

package com.dxfeed.sdk.mappers;

import com.dxfeed.schedule.Schedule;
import com.dxfeed.sdk.schedule.DxfgScheduleHandle;
import org.graalvm.nativeimage.c.struct.SizeOf;

public class ScheduleMapper extends JavaObjectHandlerMapper<Schedule, DxfgScheduleHandle> {

    @Override
    protected int getSizeJavaObjectHandler() {
        return SizeOf.get(DxfgScheduleHandle.class);
    }
}
