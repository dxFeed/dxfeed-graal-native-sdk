package com.dxfeed.event.market;

import com.dxfeed.api.NativeUtils;
import com.dxfeed.api.javac.DxfgJavaObjectHandlerList;
import com.dxfeed.api.javac.DxfgJavaObjectHandlerPointer;
import com.dxfeed.api.javac.JavaObjectHandler;
import org.graalvm.nativeimage.c.struct.SizeOf;

public class ListJavaObjectHandlerMapper extends
    ListMapper<Object, JavaObjectHandler<?>, DxfgJavaObjectHandlerPointer, DxfgJavaObjectHandlerList> {

  @Override
  protected Object toJava(final JavaObjectHandler<?> nObject) {
    return NativeUtils.toJava(nObject);
  }

  @Override
  protected JavaObjectHandler<?> toNative(final Object jObject) {
    return NativeUtils.newJavaObjectHandler(jObject);
  }

  @Override
  protected void releaseNative(final JavaObjectHandler<?> nObject) {
    NativeUtils.release(nObject);
  }

  @Override
  protected int getSizeElementInCList() {
    return SizeOf.get(JavaObjectHandler.class);
  }

  @Override
  protected int getSizeCList() {
    return SizeOf.get(DxfgJavaObjectHandlerList.class);
  }
}
