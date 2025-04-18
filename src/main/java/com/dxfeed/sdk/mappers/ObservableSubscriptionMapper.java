// Copyright (c) 2025 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

package com.dxfeed.sdk.mappers;

import com.dxfeed.api.osub.ObservableSubscription;
import com.dxfeed.event.EventType;
import com.dxfeed.sdk.publisher.DxfgObservableSubscription;
import org.graalvm.nativeimage.c.struct.SizeOf;

public class ObservableSubscriptionMapper
        extends JavaObjectHandlerMapper<ObservableSubscription<? extends EventType<?>>, DxfgObservableSubscription> {

    @Override
    protected int getSizeJavaObjectHandler() {
        return SizeOf.get(DxfgObservableSubscription.class);
    }
}
