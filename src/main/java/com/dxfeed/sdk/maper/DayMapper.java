package com.dxfeed.sdk.maper;

import com.dxfeed.schedule.Day;
import com.dxfeed.sdk.schedule.DxfgDay;
import org.graalvm.nativeimage.c.struct.SizeOf;

public class DayMapper extends JavaObjectHandlerMapper<Day, DxfgDay> {

  @Override
  protected int getSizeJavaObjectHandler() {
    return SizeOf.get(DxfgDay.class);
  }
}
