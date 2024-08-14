package com.dxfeed.event.market;

import com.dxfeed.event.IndexedEventSource;
import com.dxfeed.sdk.events.DxfgIndexedEventSourceList;
import com.dxfeed.sdk.events.DxfgIndexedEventSourcePointer;
import com.dxfeed.sdk.maper.ListMapper;
import com.dxfeed.sdk.maper.Mapper;
import com.dxfeed.sdk.source.DxfgIndexedEventSource;
import org.graalvm.nativeimage.c.struct.SizeOf;

public class ListIndexedEventSourceMapper extends
    ListMapper<IndexedEventSource, DxfgIndexedEventSource, DxfgIndexedEventSourcePointer, DxfgIndexedEventSourceList> {

  private final Mapper<IndexedEventSource, DxfgIndexedEventSource> sourceMappers;

  public ListIndexedEventSourceMapper(
      final Mapper<IndexedEventSource, DxfgIndexedEventSource> sourceMappers) {
    this.sourceMappers = sourceMappers;
  }

  @Override
  protected IndexedEventSource toJava(final DxfgIndexedEventSource nObject) {
    return sourceMappers.toJava(nObject);
  }

  @Override
  protected void releaseNative(final DxfgIndexedEventSource nObject) {
    sourceMappers.release(nObject);
  }

  @Override
  protected DxfgIndexedEventSource toNative(final IndexedEventSource jObject) {
    return sourceMappers.toNative(jObject);
  }

  @Override
  protected int getSizeCList() {
    return SizeOf.get(DxfgIndexedEventSourceList.class);
  }
}
