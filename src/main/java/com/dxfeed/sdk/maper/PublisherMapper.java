package com.dxfeed.sdk.maper;

import com.dxfeed.api.DXPublisher;
import com.dxfeed.sdk.publisher.DxfgPublisher;
import org.graalvm.nativeimage.c.struct.SizeOf;

public class PublisherMapper extends JavaObjectHandlerMapper<DXPublisher, DxfgPublisher> {

  @Override
  protected int getSizeJavaObjectHandler() {
    return SizeOf.get(DxfgPublisher.class);
  }
}
