package com.dxfeed.api.maper;

import com.dxfeed.api.javac.CList;
import com.dxfeed.api.javac.CPointerOnPointer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import org.graalvm.nativeimage.UnmanagedMemory;
import org.graalvm.word.PointerBase;
import org.graalvm.word.WordFactory;

public abstract class ListMapper<
    J,
    T extends PointerBase,
    P extends CPointerOnPointer<T>,
    L extends CList<P>
    > {

  public L toNativeList(final Collection<? extends J> jList) {
    final L dxfgEventTypeList = UnmanagedMemory.calloc(getSizeCList());
    if (jList == null || jList.isEmpty()) {
      dxfgEventTypeList.setSize(0);
      dxfgEventTypeList.setElements(WordFactory.nullPointer());
    } else {
      final P nativeEvents = UnmanagedMemory.calloc(getSizeElementInCList() * jList.size());
      int i = 0;
      for (final J jObject : jList) {
        nativeEvents.addressOf(i++).write(toNative(jObject));
      }
      dxfgEventTypeList.setSize(jList.size());
      dxfgEventTypeList.setElements(nativeEvents);
    }
    return dxfgEventTypeList;
  }

  public Collection<J> toJavaList(final L nList) {
    if (nList.isNull() || nList.getSize() == 0) {
      return Collections.emptyList();
    }
    final Collection<J> jList = new ArrayList<>(nList.getSize());
    for (int i = 0; i < nList.getSize(); ++i) {
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

  protected abstract int getSizeElementInCList();

  protected abstract int getSizeCList();
}
