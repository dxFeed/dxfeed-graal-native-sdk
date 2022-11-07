package com.dxfeed.event.market;

import org.graalvm.word.PointerBase;

public interface NativeMapper<V, T extends PointerBase> {

  T nativeObject(final V jObject);

  void delete(final T cObject);
}
