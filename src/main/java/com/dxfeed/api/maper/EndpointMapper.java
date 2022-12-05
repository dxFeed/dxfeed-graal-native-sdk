package com.dxfeed.api.maper;

import com.dxfeed.api.DXEndpoint;
import com.dxfeed.api.endpoint.DxfgEndpoint;
import org.graalvm.nativeimage.c.struct.SizeOf;

public class EndpointMapper extends JavaObjectHandlerMapper<DXEndpoint, DxfgEndpoint> {

  @Override
  protected int getSizeJavaObjectHandler() {
    return SizeOf.get(DxfgEndpoint.class);
  }
}
