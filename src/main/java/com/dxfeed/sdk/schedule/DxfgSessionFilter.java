package com.dxfeed.sdk.schedule;

import com.dxfeed.schedule.SessionFilter;
import com.dxfeed.sdk.javac.JavaObjectHandler;
import org.graalvm.nativeimage.c.CContext;
import org.graalvm.nativeimage.c.struct.CStruct;

@CContext(Directives.class)
@CStruct("dxfg_session_filter_t")
public interface DxfgSessionFilter extends JavaObjectHandler<SessionFilter> {

}
