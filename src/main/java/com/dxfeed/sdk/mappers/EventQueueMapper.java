// Copyright (c) 2026 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

package com.dxfeed.sdk.mappers;

import com.dxfeed.sdk.subscription.DxfgEventQueueHandle;
import com.dxfeed.sdk.subscription.EventQueue;
import org.graalvm.nativeimage.c.struct.SizeOf;

public class EventQueueMapper extends JavaObjectHandlerMapper<EventQueue, DxfgEventQueueHandle> {

    @Override
    protected int getSizeJavaObjectHandler() {
        return SizeOf.get(DxfgEventQueueHandle.class);
    }
}
