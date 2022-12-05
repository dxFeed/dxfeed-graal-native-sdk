package com.dxfeed.api.maper;

import com.dxfeed.api.DXFeedTimeSeriesSubscription;
import com.dxfeed.api.subscription.DxfgTimeSeriesSubscription;
import com.dxfeed.event.TimeSeriesEvent;
import org.graalvm.nativeimage.c.struct.SizeOf;

public class TimeSeriesSubscriptionMapper
    extends JavaObjectHandlerMapper<DXFeedTimeSeriesSubscription<? extends TimeSeriesEvent<?>>, DxfgTimeSeriesSubscription> {

  @Override
  protected int getSizeJavaObjectHandler() {
    return SizeOf.get(DxfgTimeSeriesSubscription.class);
  }
}
