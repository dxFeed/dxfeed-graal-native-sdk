package com.dxfeed.api.maper;

import com.dxfeed.api.Mapper;
import com.dxfeed.api.javac.DxfgJavaObjectHandlerList;
import com.dxfeed.api.javac.DxfgJavaObjectHandlerPointer;
import com.dxfeed.api.javac.JavaObjectHandler;
import com.dxfeed.event.market.ListMapper;
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
  protected int getSizeElementInCList() {
    return SizeOf.get(JavaObjectHandler.class);
  }

  @Override
  protected int getSizeCList() {
    return SizeOf.get(DxfgJavaObjectHandlerList.class);
  }
}
