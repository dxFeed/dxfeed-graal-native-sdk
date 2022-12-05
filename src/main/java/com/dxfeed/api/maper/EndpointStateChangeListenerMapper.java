package com.dxfeed.api.maper;

import com.dxfeed.api.endpoint.DxfgEndpointStateChangeListener;
import java.beans.PropertyChangeListener;
import org.graalvm.nativeimage.c.struct.SizeOf;

public class EndpointStateChangeListenerMapper
    extends JavaObjectHandlerMapper<PropertyChangeListener, DxfgEndpointStateChangeListener> {

  @Override
  protected int getSizeJavaObjectHandler() {
    return SizeOf.get(DxfgEndpointStateChangeListener.class);
  }
}
