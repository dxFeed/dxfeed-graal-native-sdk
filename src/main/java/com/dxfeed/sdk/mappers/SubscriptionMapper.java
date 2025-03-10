// Copyright (c) 2025 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

package com.dxfeed.sdk.mappers;

import com.dxfeed.api.DXFeedSubscription;
import com.dxfeed.event.EventType;
import com.dxfeed.sdk.subscription.DxfgSubscription;
import org.graalvm.nativeimage.c.struct.SizeOf;

public class SubscriptionMapper<T extends DXFeedSubscription<? extends EventType<?>>>
        extends JavaObjectHandlerMapper<T, DxfgSubscription<T>> {

    @Override
    protected int getSizeJavaObjectHandler() {
        return SizeOf.get(DxfgSubscription.class);
    }
}
