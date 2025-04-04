// Copyright (c) 2025 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

package com.dxfeed.sdk.subscription;

import com.dxfeed.api.DXFeedTimeSeriesSubscription;
import com.dxfeed.event.TimeSeriesEvent;
import org.graalvm.nativeimage.c.CContext;
import org.graalvm.nativeimage.c.struct.CStruct;

@CContext(Directives.class)
@CStruct("dxfg_time_series_subscription_t")
public interface DxfgTimeSeriesSubscription
        extends DxfgSubscription<DXFeedTimeSeriesSubscription<? extends TimeSeriesEvent<?>>> {

}