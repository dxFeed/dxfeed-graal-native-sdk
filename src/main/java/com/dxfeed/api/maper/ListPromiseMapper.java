package com.dxfeed.api.maper;

import static com.dxfeed.api.NativeUtils.MAPPER_PROMISE;

import com.dxfeed.api.feed.DxfgPromise;
import com.dxfeed.api.feed.DxfgPromiseList;
import com.dxfeed.api.feed.DxfgPromisePointer;
import com.dxfeed.promise.Promise;
import org.graalvm.nativeimage.c.struct.SizeOf;

public class ListPromiseMapper
    extends ListMapper<Promise<?>, DxfgPromise, DxfgPromisePointer, DxfgPromiseList> {

  protected final Mapper<Promise<?>, DxfgPromise> promiseMapper;

  public ListPromiseMapper(final Mapper<Promise<?>, DxfgPromise> promiseMapper) {
    this.promiseMapper = promiseMapper;
  }

  @Override
  protected Promise<?> toJava(final DxfgPromise nObject) {
    return MAPPER_PROMISE.toJava(nObject);
  }

  @Override
  protected DxfgPromise toNative(final Promise<?> jObject) {
    return MAPPER_PROMISE.toNative(jObject);
  }

  @Override
  protected void releaseNative(final DxfgPromise nObject) {
    MAPPER_PROMISE.release(nObject);
  }

  @Override
  protected int getSizeCList() {
    return SizeOf.get(DxfgPromiseList.class);
  }
}
