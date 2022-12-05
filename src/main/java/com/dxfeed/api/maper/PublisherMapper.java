package com.dxfeed.api.maper;

import com.dxfeed.api.DXPublisher;
import com.dxfeed.api.publisher.DxfgPublisher;
import org.graalvm.nativeimage.c.struct.SizeOf;

public class PublisherMapper extends JavaObjectHandlerMapper<DXPublisher, DxfgPublisher> {

  @Override
  protected int getSizeJavaObjectHandler() {
    return SizeOf.get(DxfgPublisher.class);
  }
}
