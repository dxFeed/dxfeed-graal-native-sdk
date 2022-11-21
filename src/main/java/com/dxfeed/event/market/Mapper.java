package com.dxfeed.event.market;

import org.graalvm.nativeimage.UnmanagedMemory;
import org.graalvm.word.PointerBase;

public abstract class Mapper<V, T extends PointerBase> {

  public abstract T toNativeObject(final V jObject);

  public void release(final T nObject) {
    if (nObject.isNull()) {
      return;
    }
    cleanNativeObject(nObject);
    UnmanagedMemory.free(nObject);
  }

  public abstract void fillNativeObject(final V jObject, final T nObject);

  protected abstract void cleanNativeObject(final T nObject);

  public abstract V toJavaObject(final T nObject);

  public abstract void fillJavaObject(final T nObject, final V jObject);
}
