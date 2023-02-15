package com.dxfeed.api.schedule;

import com.dxfeed.api.javac.JavaObjectHandler;
import com.dxfeed.schedule.Schedule;
import org.graalvm.nativeimage.c.CContext;
import org.graalvm.nativeimage.c.struct.CStruct;

@CContext(Directives.class)
@CStruct("dxfg_schedule_t")
public interface DxfgSchedule extends JavaObjectHandler<Schedule> {

}
