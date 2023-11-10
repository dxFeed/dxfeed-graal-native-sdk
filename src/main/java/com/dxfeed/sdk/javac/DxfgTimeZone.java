package com.dxfeed.sdk.javac;

import org.graalvm.nativeimage.c.CContext;
import org.graalvm.nativeimage.c.struct.CStruct;

import java.util.TimeZone;

@CContext(Directives.class)
@CStruct("dxfg_time_zone_t")
public interface DxfgTimeZone extends JavaObjectHandler<TimeZone> {

}
