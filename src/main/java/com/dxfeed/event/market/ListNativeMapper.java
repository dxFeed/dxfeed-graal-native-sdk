package com.dxfeed.event.market;

import java.util.List;
import org.graalvm.word.PointerBase;

public interface ListNativeMapper<V, T extends PointerBase> {

  T nativeObject(final List<V> jObjects);

  void delete(final T cObject, final int size);
}
