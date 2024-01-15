package com.dxfeed.sdk.maper;

import com.dxfeed.ondemand.OnDemandService;
import com.dxfeed.sdk.ondemand.DxfgOnDemandService;
import org.graalvm.nativeimage.c.struct.SizeOf;

public class OnDemandServiceMapper extends JavaObjectHandlerMapper<OnDemandService, DxfgOnDemandService> {

  @Override
  protected int getSizeJavaObjectHandler() {
    return SizeOf.get(DxfgOnDemandService.class);
  }
}
