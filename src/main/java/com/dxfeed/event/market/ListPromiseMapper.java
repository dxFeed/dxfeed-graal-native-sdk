package com.dxfeed.event.market;

import com.dxfeed.api.NativeUtils;
import com.dxfeed.api.feed.DxfgPromise;
import com.dxfeed.api.feed.DxfgPromiseList;
import com.dxfeed.api.feed.DxfgPromisePointer;
import com.dxfeed.promise.Promise;
import org.graalvm.nativeimage.c.struct.SizeOf;

public class ListPromiseMapper
    extends ListMapper<Promise<?>, DxfgPromise<?>, DxfgPromisePointer, DxfgPromiseList> {

  @Override
  protected Promise<?> toJava(final DxfgPromise<?> nObject) {
    return NativeUtils.toJava(nObject);
  }

  @Override
  protected DxfgPromise<?> toNative(final Promise<?> jObject) {
    return NativeUtils.newJavaObjectHandler(jObject);
  }

  @Override
  protected void releaseNative(final DxfgPromise<?> nObject) {
    NativeUtils.release(nObject);
  }

  @Override
  protected int getSizeElementInCList() {
    return SizeOf.get(DxfgPromise.class);
  }

  @Override
  protected int getSizeCList() {
    return SizeOf.get(DxfgPromiseList.class);
  }
}
