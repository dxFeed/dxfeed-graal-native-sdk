package com.dxfeed.sdk.mappers;

import com.dxfeed.sdk.NativeUtils;
import com.dxfeed.sdk.javac.JavaObjectHandler;
import org.graalvm.nativeimage.ObjectHandles;
import org.graalvm.nativeimage.UnmanagedMemory;
import org.graalvm.nativeimage.c.struct.SizeOf;
import org.graalvm.word.WordFactory;

public class JavaObjectHandlerMapper<T, V extends JavaObjectHandler<T>> extends Mapper<T, V> {

  @Override
  public V toNative(final T javaObject) {
    if (javaObject == null) {
      return WordFactory.nullPointer();
    }

    final V nativeObject = UnmanagedMemory.calloc(getSizeJavaObjectHandler());
    fillNative(javaObject, nativeObject,
        false); //There is no need to destroy the object handle since the memory has just been allocated and zeroed.

    return nativeObject;
  }

  public V toNativeArray(final T[] javaObjects) {
    if (javaObjects == null || javaObjects.length == 0) {
      return WordFactory.nullPointer();
    }

    final V nativeObject = UnmanagedMemory.calloc(getSizeJavaObjectHandler() * javaObjects.length);

    for (int i = 0; i < javaObjects.length; i++) {
      //noinspection unchecked
      fillNative(javaObjects[i], (V) nativeObject.addressOf(i), false);
    }

    return nativeObject;
  }

  public void releaseNativeArray(final V nativeArray, int size) {
    if (nativeArray.isNonNull() && size > 0) {
      for (int i = 0; i < size; i++) {
        //noinspection unchecked
        cleanNative((V) nativeArray.addressOf(i));
      }

      UnmanagedMemory.free(nativeArray);
    }
  }

  @Override
  public final void fillNative(final T javaObject, final V nativeObject, boolean clean) {
    if (clean) {
      cleanNative(nativeObject);
    }

    nativeObject.setJavaObjectHandler(ObjectHandles.getGlobal().create(javaObject));
  }

  @Override
  public final void cleanNative(final V nativeObject) {
    ObjectHandles.getGlobal().destroy(nativeObject.getJavaObjectHandler());
  }

  @Override
  protected T doToJava(final V nativeObject) {
    return ObjectHandles.getGlobal().get(nativeObject.getJavaObjectHandler());
  }

  @Override
  public void fillJava(final V nativeObject, final T javaObject) {
    throw new IllegalStateException("The Java object does not support setters.");
  }

  protected int getSizeJavaObjectHandler() {
    return SizeOf.get(JavaObjectHandler.class);
  }

}
