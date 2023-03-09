package com.dxfeed.sdk.schedule;

import com.dxfeed.schedule.Day;
import com.dxfeed.sdk.javac.JavaObjectHandler;
import org.graalvm.nativeimage.c.CContext;
import org.graalvm.nativeimage.c.struct.CStruct;

@CContext(Directives.class)
@CStruct("dxfg_day_t")
public interface DxfgDay extends JavaObjectHandler<Day> {

}
