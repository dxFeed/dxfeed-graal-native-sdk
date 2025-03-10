// Copyright (c) 2025 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

package com.dxfeed.sdk.mappers;

import com.dxfeed.schedule.SessionFilter;
import com.dxfeed.sdk.schedule.DxfgSessionFilter;
import org.graalvm.nativeimage.c.struct.SizeOf;

public class SessionFilterMapper extends JavaObjectHandlerMapper<SessionFilter, DxfgSessionFilter> {

    @Override
    protected int getSizeJavaObjectHandler() {
        return SizeOf.get(DxfgSessionFilter.class);
    }
}
