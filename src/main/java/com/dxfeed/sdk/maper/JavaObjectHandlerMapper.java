package com.dxfeed.sdk.maper;

import com.dxfeed.sdk.javac.JavaObjectHandler;
import org.graalvm.nativeimage.ObjectHandles;
import org.graalvm.nativeimage.UnmanagedMemory;
import org.graalvm.nativeimage.c.struct.SizeOf;
import org.graalvm.word.WordFactory;

public class JavaObjectHandlerMapper<T, V extends JavaObjectHandler<T>> extends Mapper<T, V> {

  @Override
  public V toNative(final T jObject) {
    if (jObject == null) {
      return WordFactory.nullPointer();
    }
    final V nObject = UnmanagedMemory.calloc(getSizeJavaObjectHandler());
    nObject.setJavaObjectHandler(ObjectHandles.getGlobal().create(jObject));
    return nObject;
  }

  @Override
  public final void fillNative(final T jObject, final V nObject) {
    cleanNative(nObject);
    nObject.setJavaObjectHandler(ObjectHandles.getGlobal().create(jObject));
  }

  @Override
  public final void cleanNative(final V nObject) {
    ObjectHandles.getGlobal().destroy(nObject.getJavaObjectHandler());
  }

  @Override
  protected T doToJava(final V nObject) {
    return ObjectHandles.getGlobal().get(nObject.getJavaObjectHandler());
  }

  @Override
  public void fillJava(final V nObject, final T jObject) {
    throw new IllegalStateException("The Java object does not support setters.");
  }

  protected int getSizeJavaObjectHandler() {
    return SizeOf.get(JavaObjectHandler.class);
  }

}
