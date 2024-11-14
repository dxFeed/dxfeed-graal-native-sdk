package com.dxfeed.sdk.mappers;

import com.dxfeed.api.DXFeedTimeSeriesSubscription;
import com.dxfeed.event.TimeSeriesEvent;
import com.dxfeed.sdk.subscription.DxfgTimeSeriesSubscription;
import org.graalvm.nativeimage.c.struct.SizeOf;

public class TimeSeriesSubscriptionMapper
    extends JavaObjectHandlerMapper<DXFeedTimeSeriesSubscription<? extends TimeSeriesEvent<?>>, DxfgTimeSeriesSubscription> {

  @Override
  protected int getSizeJavaObjectHandler() {
    return SizeOf.get(DxfgTimeSeriesSubscription.class);
  }
}
