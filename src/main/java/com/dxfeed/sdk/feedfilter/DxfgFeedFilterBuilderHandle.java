// Copyright (c) 2026 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

package com.dxfeed.sdk.feedfilter;

import com.dxfeed.api.DXFeedFilter;
import com.dxfeed.sdk.javac.JavaObjectHandler;
import org.graalvm.nativeimage.c.CContext;
import org.graalvm.nativeimage.c.struct.CStruct;

/**
 * {@code dxfg_v4_feed_filter_builder_t}: the handle of a {@link DXFeedFilter.Builder}.
 */
@CContext(FeedFilterDirectives.class)
@CStruct("dxfg_v4_feed_filter_builder_t")
public interface DxfgFeedFilterBuilderHandle extends JavaObjectHandler<DXFeedFilter.Builder> {
}
