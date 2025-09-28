// Copyright (c) 2025 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

package com.dxfeed.sdk.subscriptioncontroller;

import com.dxfeed.api.SubscriptionController;
import com.dxfeed.sdk.javac.JavaObjectHandler;
import org.graalvm.nativeimage.c.CContext;
import org.graalvm.nativeimage.c.struct.CStruct;

@CContext(Directives.class)
@CStruct("dxfg_subscription_controller_t")
public interface DxfgSubscriptionControllerHandle extends JavaObjectHandler<SubscriptionController> {

}
