package com.dxfeed.api.maper;

import com.dxfeed.api.DXFeedSubscription;
import com.dxfeed.api.subscription.DxfgSubscription;
import com.dxfeed.event.EventType;
import org.graalvm.nativeimage.c.struct.SizeOf;

public class SubscriptionMapper<T extends DXFeedSubscription<? extends EventType<?>>>
    extends JavaObjectHandlerMapper<T, DxfgSubscription<T>> {

  @Override
  protected int getSizeJavaObjectHandler() {
    return SizeOf.get(DxfgSubscription.class);
  }
}
