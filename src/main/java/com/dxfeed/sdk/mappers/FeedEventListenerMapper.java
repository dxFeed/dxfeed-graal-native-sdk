// Copyright (c) 2025 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

package com.dxfeed.sdk.mappers;

import com.dxfeed.api.DXFeedEventListener;
import com.dxfeed.event.EventType;
import com.dxfeed.sdk.subscription.DxfgFeedEventListener;
import org.graalvm.nativeimage.c.struct.SizeOf;

public class FeedEventListenerMapper
        extends JavaObjectHandlerMapper<DXFeedEventListener<EventType<?>>, DxfgFeedEventListener> {

    @Override
    protected int getSizeJavaObjectHandler() {
        return SizeOf.get(DxfgFeedEventListener.class);
    }
}
