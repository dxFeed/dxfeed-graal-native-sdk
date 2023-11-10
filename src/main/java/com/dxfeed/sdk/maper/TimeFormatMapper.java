package com.dxfeed.sdk.maper;

import com.devexperts.util.TimeFormat;
import com.dxfeed.sdk.javac.DxfgTimeFormat;
import org.graalvm.nativeimage.c.struct.SizeOf;

public class TimeFormatMapper extends JavaObjectHandlerMapper<TimeFormat, DxfgTimeFormat> {

  @Override
  protected int getSizeJavaObjectHandler() {
    return SizeOf.get(DxfgTimeFormat.class);
  }
}
