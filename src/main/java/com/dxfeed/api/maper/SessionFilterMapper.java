package com.dxfeed.api.maper;

import com.dxfeed.api.schedule.DxfgSessionFilter;
import com.dxfeed.schedule.SessionFilter;
import org.graalvm.nativeimage.c.struct.SizeOf;

public class SessionFilterMapper extends JavaObjectHandlerMapper<SessionFilter, DxfgSessionFilter> {

  @Override
  protected int getSizeJavaObjectHandler() {
    return SizeOf.get(DxfgSessionFilter.class);
  }
}
