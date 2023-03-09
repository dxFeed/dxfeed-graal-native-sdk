package com.dxfeed.sdk.schedule;

import com.dxfeed.schedule.Session;
import com.dxfeed.sdk.javac.JavaObjectHandler;
import org.graalvm.nativeimage.c.CContext;
import org.graalvm.nativeimage.c.struct.CStruct;

@CContext(Directives.class)
@CStruct("dxfg_session_t")
public interface DxfgSession extends JavaObjectHandler<Session> {

}
