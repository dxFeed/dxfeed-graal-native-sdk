// Copyright (c) 2024 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

package com.dxfeed.sdk.mappers;

import com.dxfeed.sdk.javac.CList;
import com.dxfeed.sdk.javac.CPointerPointer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import org.graalvm.nativeimage.UnmanagedMemory;
import org.graalvm.word.PointerBase;
import org.graalvm.word.WordFactory;

public abstract class ListMapper<
    J,
    T extends PointerBase,
    P extends CPointerPointer<T>,
    L extends CList<P>
    > {

  //https://github.com/graalvm/graal-jvmci-8/blob/master/jvmci/jdk.vm.ci.code/src/jdk/vm/ci/code/TargetDescription.java
  //ConfigurationValues.getTarget().arch.getWordSize() or ConfigurationValues.getTarget().wordSize;
  protected static final int SIZE_OF_C_POINTER = 8;

  public L toNativeList(final Collection<? extends J> jList) {
    final L dxfgEventTypeList = UnmanagedMemory.calloc(getSizeCList());
    if (jList == null || jList.isEmpty()) {
      dxfgEventTypeList.setSize(0);
      dxfgEventTypeList.setElements(WordFactory.nullPointer());
    } else {
      final P nativeEvents = UnmanagedMemory.calloc(SIZE_OF_C_POINTER * jList.size());
      int i = 0;
      for (final J jObject : jList) {
        nativeEvents.addressOf(i++).write(toNative(jObject));
      }
      dxfgEventTypeList.setSize(jList.size());
      dxfgEventTypeList.setElements(nativeEvents);
    }
    return dxfgEventTypeList;
  }

  public List<J> toJavaList(final L nList) {
    if (nList.isNull() || nList.getSize() == 0) {
      return Collections.emptyList();
    }
    final List<J> jList = new ArrayList<>(nList.getSize());
    for (int i = 0; i < nList.getSize(); i++) {
      jList.add(toJava(nList.getElements().addressOf(i).read()));
    }
    return jList;
  }

  public void release(final L nList) {
    if (nList.isNull()) {
      return;
    }
    if (nList.getElements().isNonNull()) {
      for (int i = 0; i < nList.getSize(); ++i) {
        final T nObject = nList.getElements().addressOf(i).read();
        if (nObject.isNonNull()) {
          releaseNative(nObject);
        }
      }
      UnmanagedMemory.free(nList.getElements());
    }
    UnmanagedMemory.free(nList);
  }

  protected abstract J toJava(final T nObject);

  protected abstract T toNative(final J jObject);

  protected abstract void releaseNative(final T nObject);

  protected abstract int getSizeCList();
}
