// Copyright (c) 2025 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

package com.dxfeed.sdk.subscription;

import com.dxfeed.api.DXFeedEventListener;
import com.dxfeed.event.EventType;
import com.dxfeed.sdk.javac.JavaObjectHandler;
import org.graalvm.nativeimage.c.CContext;
import org.graalvm.nativeimage.c.struct.CStruct;

@CContext(Directives.class)
@CStruct("dxfg_feed_event_listener_t")
public interface DxfgFeedEventListener
        extends JavaObjectHandler<DXFeedEventListener<EventType<?>>> {

}
