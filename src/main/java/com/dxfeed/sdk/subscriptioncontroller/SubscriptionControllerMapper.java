// Copyright (c) 2025 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

package com.dxfeed.sdk.subscriptioncontroller;

import com.dxfeed.api.SubscriptionController;
import com.dxfeed.sdk.mappers.JavaObjectHandlerMapper;
import org.graalvm.nativeimage.c.struct.SizeOf;

public class SubscriptionControllerMapper extends
        JavaObjectHandlerMapper<SubscriptionController, DxfgSubscriptionControllerHandle> {

    @Override
    protected int getSizeJavaObjectHandler() {
        return SizeOf.get(DxfgSubscriptionControllerHandle.class);
    }
}
