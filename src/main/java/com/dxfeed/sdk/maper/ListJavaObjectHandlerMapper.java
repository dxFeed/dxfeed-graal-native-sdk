package com.dxfeed.sdk.maper;

import com.dxfeed.sdk.javac.DxfgJavaObjectHandlerList;
import com.dxfeed.sdk.javac.DxfgJavaObjectHandlerPointer;
import com.dxfeed.sdk.javac.JavaObjectHandler;
import org.graalvm.nativeimage.c.struct.SizeOf;

public class ListJavaObjectHandlerMapper
    extends ListMapper<Object, JavaObjectHandler<?>, DxfgJavaObjectHandlerPointer, DxfgJavaObjectHandlerList> {

  private final Mapper<Object, JavaObjectHandler<Object>> mapperJavaObjectHandler;

  public ListJavaObjectHandlerMapper(
      final Mapper<Object, JavaObjectHandler<Object>> mapperJavaObjectHandler
  ) {
    this.mapperJavaObjectHandler = mapperJavaObjectHandler;
  }

  @Override
  protected Object toJava(final JavaObjectHandler<?> nObject) {
    return mapperJavaObjectHandler.toJava((JavaObjectHandler<Object>) nObject);
  }

  @Override
  protected JavaObjectHandler<?> toNative(final Object jObject) {
    return mapperJavaObjectHandler.toNative(jObject);
  }

  @Override
  protected void releaseNative(final JavaObjectHandler<?> nObject) {
    mapperJavaObjectHandler.release((JavaObjectHandler<Object>) nObject);
  }

  @Override
  protected int getSizeCList() {
    return SizeOf.get(DxfgJavaObjectHandlerList.class);
  }
}
