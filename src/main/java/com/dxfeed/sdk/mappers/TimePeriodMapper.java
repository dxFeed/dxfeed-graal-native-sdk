// Copyright (c) 2025 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

package com.dxfeed.sdk.mappers;

import com.devexperts.util.TimePeriod;
import com.dxfeed.sdk.javac.DxfgTimePeriod;
import org.graalvm.nativeimage.c.struct.SizeOf;

public class TimePeriodMapper extends JavaObjectHandlerMapper<TimePeriod, DxfgTimePeriod> {

    @Override
    protected int getSizeJavaObjectHandler() {
        return SizeOf.get(DxfgTimePeriod.class);
    }
}
