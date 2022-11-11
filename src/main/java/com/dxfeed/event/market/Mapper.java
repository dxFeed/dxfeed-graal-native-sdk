package com.dxfeed.event.market;

import org.graalvm.nativeimage.UnmanagedMemory;
import org.graalvm.word.PointerBase;
import org.graalvm.word.WordFactory;

public abstract class Mapper<V, T extends PointerBase> {

  public T nativeObject(final V jObject) {
    if (jObject == null) {
      return WordFactory.nullPointer();
    }
    final T nObject = UnmanagedMemory.calloc(size());
    fillNativeObject(nObject, jObject);
    return nObject;
  }

  public void delete(final T nObject) {
    if (nObject.isNull()) {
      return;
    }
    cleanNativeObject(nObject);
    UnmanagedMemory.free(nObject);
  }

  protected abstract int size();

  protected abstract void fillNativeObject(final T nObject, final V jObject);

  protected abstract void cleanNativeObject(final T nObject);
}
