package com.dxfeed.api.schedule;

import com.dxfeed.api.javac.JavaObjectHandler;
import com.dxfeed.schedule.DayFilter;
import org.graalvm.nativeimage.c.CContext;
import org.graalvm.nativeimage.c.struct.CStruct;

@CContext(Directives.class)
@CStruct("dxfg_day_filter_t")
public interface DxfgDayFilter extends JavaObjectHandler<DayFilter> {

}
