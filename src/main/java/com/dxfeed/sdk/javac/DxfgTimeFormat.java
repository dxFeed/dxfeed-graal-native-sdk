package com.dxfeed.sdk.javac;

import com.devexperts.util.TimeFormat;
import org.graalvm.nativeimage.c.CContext;
import org.graalvm.nativeimage.c.struct.CStruct;

@CContext(Directives.class)
@CStruct("dxfg_time_format_t")
public interface DxfgTimeFormat extends JavaObjectHandler<TimeFormat> {

}
