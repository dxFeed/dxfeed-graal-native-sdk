package com.dxfeed.sdk.maper;

import com.dxfeed.api.osub.ObservableSubscriptionChangeListener;
import com.dxfeed.sdk.events.DxfgObservableSubscriptionChangeListener;
import org.graalvm.nativeimage.c.struct.SizeOf;

public class ObservableSubscriptionChangeListenerMapper
    extends JavaObjectHandlerMapper<ObservableSubscriptionChangeListener, DxfgObservableSubscriptionChangeListener> {

  @Override
  protected int getSizeJavaObjectHandler() {
    return SizeOf.get(DxfgObservableSubscriptionChangeListener.class);
  }
}
