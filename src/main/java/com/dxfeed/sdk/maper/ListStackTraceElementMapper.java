package com.dxfeed.sdk.maper;

import com.dxfeed.sdk.exception.DxfgStackTraceElement;
import com.dxfeed.sdk.exception.DxfgStackTraceElementList;
import com.dxfeed.sdk.exception.DxfgStackTraceElementPointer;
import org.graalvm.nativeimage.c.struct.SizeOf;

public class ListStackTraceElementMapper
    extends ListMapper<StackTraceElement, DxfgStackTraceElement, DxfgStackTraceElementPointer, DxfgStackTraceElementList> {

  private final Mapper<StackTraceElement, DxfgStackTraceElement> mapper;

  public ListStackTraceElementMapper(final Mapper<StackTraceElement, DxfgStackTraceElement> mapper) {
    this.mapper = mapper;
  }

  @Override
  protected StackTraceElement toJava(final DxfgStackTraceElement nObject) {
    return this.mapper.toJava(nObject);
  }

  @Override
  protected DxfgStackTraceElement toNative(final StackTraceElement jObject) {
    return this.mapper.toNative(jObject);
  }

  @Override
  protected void releaseNative(final DxfgStackTraceElement nObject) {
    this.mapper.release(nObject);
  }

  @Override
  protected int getSizeCList() {
    return SizeOf.get(DxfgStackTraceElementList.class);
  }
}
