// Copyright (c) 2025 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

package com.dxfeed.sdk.mappers;

import com.dxfeed.schedule.Session;
import com.dxfeed.sdk.schedule.DxfgSession;
import org.graalvm.nativeimage.c.struct.SizeOf;

public class SessionMapper extends JavaObjectHandlerMapper<Session, DxfgSession> {

    @Override
    protected int getSizeJavaObjectHandler() {
        return SizeOf.get(DxfgSession.class);
    }
}
