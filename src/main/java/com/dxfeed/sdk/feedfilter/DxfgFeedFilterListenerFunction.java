// Copyright (c) 2026 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

package com.dxfeed.sdk.feedfilter;

import org.graalvm.nativeimage.IsolateThread;
import org.graalvm.nativeimage.c.CContext;
import org.graalvm.nativeimage.c.function.CFunctionPointer;
import org.graalvm.nativeimage.c.function.InvokeCFunctionPointer;
import org.graalvm.nativeimage.c.type.CTypedef;
import org.graalvm.nativeimage.c.type.VoidPointer;

/**
 * {@code dxfg_v4_feed_filter_listener_func}: the function of a listener of the updates of a feed filter.
 */
@CContext(FeedFilterDirectives.class)
@CTypedef(name = "dxfg_v4_feed_filter_listener_func")
interface DxfgFeedFilterListenerFunction extends CFunctionPointer {

    @InvokeCFunctionPointer
    void invoke(final IsolateThread thread, final VoidPointer userData);
}
