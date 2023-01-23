package com.dxfeed.api.maper;

import com.dxfeed.api.ipf.DxfgIpfConnectionStateChangeListener;
import java.beans.PropertyChangeListener;
import org.graalvm.nativeimage.c.struct.SizeOf;

public class IpfConnectionStateChangeListenerMapper
    extends JavaObjectHandlerMapper<PropertyChangeListener, DxfgIpfConnectionStateChangeListener> {

  @Override
  protected int getSizeJavaObjectHandler() {
    return SizeOf.get(DxfgIpfConnectionStateChangeListener.class);
  }
}
