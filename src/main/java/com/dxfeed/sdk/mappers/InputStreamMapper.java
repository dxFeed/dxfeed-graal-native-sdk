package com.dxfeed.sdk.mappers;

import com.dxfeed.sdk.javac.DxfgInputStream;
import java.io.InputStream;
import org.graalvm.nativeimage.c.struct.SizeOf;

public class InputStreamMapper extends JavaObjectHandlerMapper<InputStream, DxfgInputStream> {

  @Override
  protected int getSizeJavaObjectHandler() {
    return SizeOf.get(DxfgInputStream.class);
  }
}
