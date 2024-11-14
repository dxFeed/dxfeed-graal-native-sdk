package com.dxfeed.sdk.mappers;

import com.dxfeed.api.DXEndpoint;
import com.dxfeed.sdk.endpoint.DxfgEndpointBuilder;
import org.graalvm.nativeimage.c.struct.SizeOf;

public class EndpointBuilderMapper
    extends JavaObjectHandlerMapper<DXEndpoint.Builder, DxfgEndpointBuilder> {

  @Override
  protected int getSizeJavaObjectHandler() {
    return SizeOf.get(DxfgEndpointBuilder.class);
  }
}
