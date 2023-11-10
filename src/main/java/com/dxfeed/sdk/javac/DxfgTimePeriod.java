package com.dxfeed.sdk.javac;

import com.devexperts.util.TimePeriod;
import org.graalvm.nativeimage.c.CContext;
import org.graalvm.nativeimage.c.struct.CStruct;

@CContext(Directives.class)
@CStruct("dxfg_time_period_t")
public interface DxfgTimePeriod extends JavaObjectHandler<TimePeriod> {

}
