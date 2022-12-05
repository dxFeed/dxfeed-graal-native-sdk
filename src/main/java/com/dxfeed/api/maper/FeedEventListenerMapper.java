package com.dxfeed.api.maper;

import com.dxfeed.api.DXFeedEventListener;
import com.dxfeed.api.subscription.DxfgFeedEventListener;
import com.dxfeed.event.EventType;
import org.graalvm.nativeimage.c.struct.SizeOf;

public class FeedEventListenerMapper
    extends JavaObjectHandlerMapper<DXFeedEventListener<EventType<?>>, DxfgFeedEventListener> {

  @Override
  protected int getSizeJavaObjectHandler() {
    return SizeOf.get(DxfgFeedEventListener.class);
  }
}
