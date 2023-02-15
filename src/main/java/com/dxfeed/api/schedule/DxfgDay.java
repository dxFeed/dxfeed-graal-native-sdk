package com.dxfeed.api.schedule;

import com.dxfeed.api.javac.JavaObjectHandler;
import com.dxfeed.schedule.Day;
import org.graalvm.nativeimage.c.CContext;
import org.graalvm.nativeimage.c.struct.CStruct;

@CContext(Directives.class)
@CStruct("dxfg_day_t")
public interface DxfgDay extends JavaObjectHandler<Day> {

}
