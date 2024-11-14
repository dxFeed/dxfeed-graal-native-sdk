package com.dxfeed.sdk.mappers;

import com.dxfeed.sdk.javac.DxfgTimeZone;
import org.graalvm.nativeimage.c.struct.SizeOf;

import java.util.TimeZone;

public class TimeZoneMapper extends JavaObjectHandlerMapper<TimeZone, DxfgTimeZone> {

  @Override
  protected int getSizeJavaObjectHandler() {
    return SizeOf.get(DxfgTimeZone.class);
  }
}
