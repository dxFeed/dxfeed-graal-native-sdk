package com.dxfeed.sdk.mappers;

import com.dxfeed.model.IndexedEventModel;
import com.dxfeed.sdk.model.DxfgIndexedEventModel;
import org.graalvm.nativeimage.c.struct.SizeOf;

public class IndexedEventModelMapper
    extends JavaObjectHandlerMapper<IndexedEventModel<?>, DxfgIndexedEventModel> {

  @Override
  protected int getSizeJavaObjectHandler() {
    return SizeOf.get(DxfgIndexedEventModel.class);
  }
}
