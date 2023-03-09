package com.dxfeed.sdk.maper;

import com.dxfeed.schedule.Schedule;
import com.dxfeed.sdk.schedule.DxfgSchedule;
import org.graalvm.nativeimage.c.struct.SizeOf;

public class ScheduleMapper extends JavaObjectHandlerMapper<Schedule, DxfgSchedule> {

  @Override
  protected int getSizeJavaObjectHandler() {
    return SizeOf.get(DxfgSchedule.class);
  }
}
