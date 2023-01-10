package com.dxfeed.api.maper;

import com.dxfeed.api.model.DxfgIndexedEventModel;
import com.dxfeed.model.IndexedEventModel;
import org.graalvm.nativeimage.c.struct.SizeOf;

public class IndexedEventModelMapper
    extends JavaObjectHandlerMapper<IndexedEventModel<?>, DxfgIndexedEventModel> {

  @Override
  protected int getSizeJavaObjectHandler() {
    return SizeOf.get(DxfgIndexedEventModel.class);
  }
}
