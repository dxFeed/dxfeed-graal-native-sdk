package com.dxfeed.api.maper;

import com.dxfeed.api.DXEndpoint;
import com.dxfeed.api.endpoint.DxfgEndpointBuilder;
import org.graalvm.nativeimage.c.struct.SizeOf;

public class EndpointBuilderMapper
    extends JavaObjectHandlerMapper<DXEndpoint.Builder, DxfgEndpointBuilder> {

  @Override
  protected int getSizeJavaObjectHandler() {
    return SizeOf.get(DxfgEndpointBuilder.class);
  }
}
