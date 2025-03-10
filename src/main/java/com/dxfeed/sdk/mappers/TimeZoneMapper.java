// Copyright (c) 2025 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

package com.dxfeed.sdk.mappers;

import com.dxfeed.sdk.javac.DxfgTimeZone;
import java.util.TimeZone;
import org.graalvm.nativeimage.c.struct.SizeOf;

public class TimeZoneMapper extends JavaObjectHandlerMapper<TimeZone, DxfgTimeZone> {

    @Override
    protected int getSizeJavaObjectHandler() {
        return SizeOf.get(DxfgTimeZone.class);
    }
}
