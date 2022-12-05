package com.dxfeed.api.maper;

import com.dxfeed.api.osub.ObservableSubscription;
import com.dxfeed.api.publisher.DxfgObservableSubscription;
import com.dxfeed.event.EventType;
import org.graalvm.nativeimage.c.struct.SizeOf;

public class ObservableSubscriptionMapper
    extends JavaObjectHandlerMapper<ObservableSubscription<? extends EventType<?>>, DxfgObservableSubscription> {

  @Override
  protected int getSizeJavaObjectHandler() {
    return SizeOf.get(DxfgObservableSubscription.class);
  }
}
