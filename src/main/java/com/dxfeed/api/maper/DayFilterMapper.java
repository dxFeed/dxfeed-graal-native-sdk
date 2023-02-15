package com.dxfeed.api.maper;

import com.dxfeed.api.schedule.DxfgDayFilter;
import com.dxfeed.schedule.DayFilter;
import org.graalvm.nativeimage.c.struct.SizeOf;

public class DayFilterMapper extends JavaObjectHandlerMapper<DayFilter, DxfgDayFilter> {

  @Override
  protected int getSizeJavaObjectHandler() {
    return SizeOf.get(DxfgDayFilter.class);
  }
}
