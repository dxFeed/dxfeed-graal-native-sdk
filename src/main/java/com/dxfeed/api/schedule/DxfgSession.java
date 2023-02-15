package com.dxfeed.api.schedule;

import com.dxfeed.api.javac.JavaObjectHandler;
import com.dxfeed.schedule.Session;
import org.graalvm.nativeimage.c.CContext;
import org.graalvm.nativeimage.c.struct.CStruct;

@CContext(Directives.class)
@CStruct("dxfg_session_t")
public interface DxfgSession extends JavaObjectHandler<Session> {

}
