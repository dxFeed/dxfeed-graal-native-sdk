package com.dxfeed.api.maper;

import com.dxfeed.api.feed.DxfgPromise;
import com.dxfeed.promise.Promise;
import org.graalvm.nativeimage.c.struct.SizeOf;

public class PromiseMapper extends JavaObjectHandlerMapper<Promise<?>, DxfgPromise> {

  @Override
  protected int getSizeJavaObjectHandler() {
    return SizeOf.get(DxfgPromise.class);
  }
}
