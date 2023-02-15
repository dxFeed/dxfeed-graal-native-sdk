package com.dxfeed.api.schedule;

import com.dxfeed.api.javac.JavaObjectHandler;
import com.dxfeed.schedule.SessionFilter;
import org.graalvm.nativeimage.c.CContext;
import org.graalvm.nativeimage.c.struct.CStruct;

@CContext(Directives.class)
@CStruct("dxfg_session_filter_t")
public interface DxfgSessionFilter extends JavaObjectHandler<SessionFilter> {

}
