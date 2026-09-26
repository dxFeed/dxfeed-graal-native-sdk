// Copyright (c) 2025 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

package com.dxfeed.sdk.feed;

import com.dxfeed.api.DXFeed;
import com.dxfeed.sdk.javac.JavaObjectHandler;
import org.graalvm.nativeimage.c.CContext;
import org.graalvm.nativeimage.c.struct.CStruct;

@CContext(FeedDirectives.class)
@CStruct("dxfg_feed_t")
public interface DxfgFeed extends JavaObjectHandler<DXFeed> {

}
