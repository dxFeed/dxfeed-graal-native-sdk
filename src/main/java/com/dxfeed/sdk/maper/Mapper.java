package com.dxfeed.sdk.maper;

import org.graalvm.nativeimage.UnmanagedMemory;
import org.graalvm.word.PointerBase;

public abstract class Mapper<V, T extends PointerBase> {

  public abstract T toNative(final V jObject);

  public void release(final T nObject) {
    if (nObject.isNull()) {
      return;
    }
    cleanNative(nObject);
    UnmanagedMemory.free(nObject);
  }

  public abstract void fillNative(final V jObject, final T nObject);

  public abstract void cleanNative(final T nObject);

  public final V toJava(final T nObject) {
    if (nObject.isNull()) {
      return null;
    }
    return doToJava(nObject);
  }

  protected abstract V doToJava(final T nObject);

  public abstract void fillJava(final T nObject, final V jObject);
}
