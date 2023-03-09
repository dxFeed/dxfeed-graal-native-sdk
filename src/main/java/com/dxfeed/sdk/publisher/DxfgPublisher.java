package com.dxfeed.sdk.publisher;

import com.dxfeed.api.DXPublisher;
import com.dxfeed.sdk.javac.JavaObjectHandler;
import org.graalvm.nativeimage.c.CContext;
import org.graalvm.nativeimage.c.struct.CStruct;

@CContext(Directives.class)
@CStruct("dxfg_publisher_t")
public interface DxfgPublisher extends JavaObjectHandler<DXPublisher> {

}
