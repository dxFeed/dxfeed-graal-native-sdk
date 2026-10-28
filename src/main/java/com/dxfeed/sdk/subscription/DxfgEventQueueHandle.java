// Copyright (c) 2026 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

package com.dxfeed.sdk.subscription;

import com.dxfeed.sdk.javac.JavaObjectHandler;
import org.graalvm.nativeimage.c.CContext;
import org.graalvm.nativeimage.c.struct.CStruct;

/**
 * {@code dxfg_v4_event_queue_t}: the handle of an {@link EventQueue}.
 */
@CContext(EventQueueDirectives.class)
@CStruct("dxfg_v4_event_queue_t")
public interface DxfgEventQueueHandle extends JavaObjectHandler<EventQueue> {
}
