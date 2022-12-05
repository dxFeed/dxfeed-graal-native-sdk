package com.dxfeed.api.endpoint;

import com.dxfeed.api.DXEndpoint;
import com.dxfeed.api.javac.JavaObjectHandler;
import org.graalvm.nativeimage.c.CContext;
import org.graalvm.nativeimage.c.struct.CStruct;

@CContext(Directives.class)
@CStruct("dxfg_endpoint_builder_t")
public interface DxfgEndpointBuilder extends JavaObjectHandler<DXEndpoint.Builder> {

}
