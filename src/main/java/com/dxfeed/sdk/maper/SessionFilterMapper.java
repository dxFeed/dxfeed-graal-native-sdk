package com.dxfeed.sdk.maper;

import com.dxfeed.schedule.SessionFilter;
import com.dxfeed.sdk.schedule.DxfgSessionFilter;
import org.graalvm.nativeimage.c.struct.SizeOf;

public class SessionFilterMapper extends JavaObjectHandlerMapper<SessionFilter, DxfgSessionFilter> {

  @Override
  protected int getSizeJavaObjectHandler() {
    return SizeOf.get(DxfgSessionFilter.class);
  }
}
