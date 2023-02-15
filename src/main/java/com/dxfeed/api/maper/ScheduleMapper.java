package com.dxfeed.api.maper;

import com.dxfeed.api.schedule.DxfgSchedule;
import com.dxfeed.schedule.Schedule;
import org.graalvm.nativeimage.c.struct.SizeOf;

public class ScheduleMapper extends JavaObjectHandlerMapper<Schedule, DxfgSchedule> {

  @Override
  protected int getSizeJavaObjectHandler() {
    return SizeOf.get(DxfgSchedule.class);
  }
}
