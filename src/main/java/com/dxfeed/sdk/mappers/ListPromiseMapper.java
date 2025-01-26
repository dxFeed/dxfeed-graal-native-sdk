package com.dxfeed.sdk.mappers;

import com.dxfeed.promise.Promise;
import com.dxfeed.sdk.NativeUtils;
import com.dxfeed.sdk.feed.DxfgPromise;
import com.dxfeed.sdk.feed.DxfgPromiseList;
import com.dxfeed.sdk.feed.DxfgPromisePointer;
import org.graalvm.nativeimage.c.struct.SizeOf;

public class ListPromiseMapper
    extends ListMapper<Promise<?>, DxfgPromise, DxfgPromisePointer, DxfgPromiseList> {

  protected final Mapper<Promise<?>, DxfgPromise> promiseMapper;

  public ListPromiseMapper(final Mapper<Promise<?>, DxfgPromise> promiseMapper) {
    this.promiseMapper = promiseMapper;
  }

  @Override
  protected Promise<?> toJava(final DxfgPromise nObject) {
    return NativeUtils.MAPPER_PROMISE.toJava(nObject);
  }

  @Override
  protected DxfgPromise toNative(final Promise<?> jObject) {
    return NativeUtils.MAPPER_PROMISE.toNative(jObject);
  }

  @Override
  protected void releaseNative(final DxfgPromise nObject) {
    NativeUtils.MAPPER_PROMISE.release(nObject);
  }

  @Override
  protected int getNativeListSize() {
    return SizeOf.get(DxfgPromiseList.class);
  }
}
