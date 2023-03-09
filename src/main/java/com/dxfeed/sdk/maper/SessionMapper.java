package com.dxfeed.sdk.maper;

import com.dxfeed.schedule.Session;
import com.dxfeed.sdk.schedule.DxfgSession;
import org.graalvm.nativeimage.c.struct.SizeOf;

public class SessionMapper extends JavaObjectHandlerMapper<Session, DxfgSession> {

  @Override
  protected int getSizeJavaObjectHandler() {
    return SizeOf.get(DxfgSession.class);
  }
}
