package com.dxfeed.api.maper;

import com.dxfeed.api.events.DxfgObservableSubscriptionChangeListener;
import com.dxfeed.api.osub.ObservableSubscriptionChangeListener;
import org.graalvm.nativeimage.c.struct.SizeOf;

public class ObservableSubscriptionChangeListenerMapper
    extends JavaObjectHandlerMapper<ObservableSubscriptionChangeListener, DxfgObservableSubscriptionChangeListener> {

  @Override
  protected int getSizeJavaObjectHandler() {
    return SizeOf.get(DxfgObservableSubscriptionChangeListener.class);
  }
}
