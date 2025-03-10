// Copyright (c) 2025 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

package com.dxfeed.sdk.subscription;

import com.dxfeed.api.DXFeedSubscription;
import com.dxfeed.event.EventType;
import com.dxfeed.sdk.javac.JavaObjectHandler;
import org.graalvm.nativeimage.c.CContext;
import org.graalvm.nativeimage.c.struct.CStruct;

@CContext(Directives.class)
@CStruct("dxfg_subscription_t")
public interface DxfgSubscription<T extends DXFeedSubscription<? extends EventType<?>>>
        extends JavaObjectHandler<T> {

}