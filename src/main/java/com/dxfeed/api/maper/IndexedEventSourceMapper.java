package com.dxfeed.api.maper;

import com.dxfeed.api.events.DxfgIndexedEventSource;
import com.dxfeed.event.IndexedEventSource;
import org.graalvm.nativeimage.c.struct.SizeOf;

public class IndexedEventSourceMapper
    extends JavaObjectHandlerMapper<IndexedEventSource, DxfgIndexedEventSource> {

  @Override
  protected int getSizeJavaObjectHandler() {
    return SizeOf.get(DxfgIndexedEventSource.class);
  }
}
