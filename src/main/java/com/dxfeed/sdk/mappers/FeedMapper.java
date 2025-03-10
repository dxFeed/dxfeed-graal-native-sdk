// Copyright (c) 2025 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

package com.dxfeed.sdk.mappers;

import com.dxfeed.api.DXFeed;
import com.dxfeed.sdk.feed.DxfgFeed;
import org.graalvm.nativeimage.c.struct.SizeOf;

public class FeedMapper extends JavaObjectHandlerMapper<DXFeed, DxfgFeed> {

    @Override
    protected int getSizeJavaObjectHandler() {
        return SizeOf.get(DxfgFeed.class);
    }
}
