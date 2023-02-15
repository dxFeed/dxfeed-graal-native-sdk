package com.dxfeed.api.maper;

import com.dxfeed.api.schedule.DxfgDay;
import com.dxfeed.schedule.Day;
import org.graalvm.nativeimage.c.struct.SizeOf;

public class DayMapper extends JavaObjectHandlerMapper<Day, DxfgDay> {

  @Override
  protected int getSizeJavaObjectHandler() {
    return SizeOf.get(DxfgDay.class);
  }
}
