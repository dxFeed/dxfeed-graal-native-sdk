package com.dxfeed.sdk.mappers;

import com.dxfeed.promise.Promise;
import com.dxfeed.sdk.feed.DxfgPromise;
import org.graalvm.nativeimage.c.struct.SizeOf;

public class PromiseMapper extends JavaObjectHandlerMapper<Promise<?>, DxfgPromise> {

  @Override
  protected int getSizeJavaObjectHandler() {
    return SizeOf.get(DxfgPromise.class);
  }
}
