package com.dxfeed.api.maper;

import com.dxfeed.api.schedule.DxfgSession;
import com.dxfeed.schedule.Session;
import org.graalvm.nativeimage.c.struct.SizeOf;

public class SessionMapper extends JavaObjectHandlerMapper<Session, DxfgSession> {

  @Override
  protected int getSizeJavaObjectHandler() {
    return SizeOf.get(DxfgSession.class);
  }
}
