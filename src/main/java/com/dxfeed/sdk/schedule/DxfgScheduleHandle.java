package com.dxfeed.sdk.schedule;

import com.dxfeed.schedule.Schedule;
import com.dxfeed.sdk.javac.JavaObjectHandler;
import org.graalvm.nativeimage.c.CContext;
import org.graalvm.nativeimage.c.struct.CStruct;

@CContext(Directives.class)
@CStruct("dxfg_schedule_t")
public interface DxfgScheduleHandle extends JavaObjectHandler<Schedule> {

}
