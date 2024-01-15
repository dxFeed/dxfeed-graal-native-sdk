package com.dxfeed.sdk.ondemand;

import com.dxfeed.ondemand.OnDemandService;
import com.dxfeed.sdk.javac.JavaObjectHandler;
import org.graalvm.nativeimage.c.CContext;
import org.graalvm.nativeimage.c.struct.CStruct;

@CContext(Directives.class)
@CStruct("dxfg_on_demand_service_t")
public interface DxfgOnDemandService extends JavaObjectHandler<OnDemandService> {

}
