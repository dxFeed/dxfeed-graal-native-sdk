package com.dxfeed.sdk.schedule;

import com.dxfeed.schedule.DayFilter;
import com.dxfeed.sdk.javac.JavaObjectHandler;
import org.graalvm.nativeimage.c.CContext;
import org.graalvm.nativeimage.c.struct.CStruct;

@CContext(Directives.class)
@CStruct("dxfg_day_filter_t")
public interface DxfgDayFilter extends JavaObjectHandler<DayFilter> {

}
