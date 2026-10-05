package com.dxfeed.sdk.model.bridge;

import com.dxfeed.api.experimental.model.IndexedTxModel;
import com.dxfeed.sdk.mappers.JavaObjectHandlerMapper;
import org.graalvm.nativeimage.c.struct.SizeOf;

public class IndexedTxModelMapper extends JavaObjectHandlerMapper<IndexedTxModel<?>, IndexedTxModelCStruct> {
  @Override
  protected int getSizeJavaObjectHandler() {
    return SizeOf.get(IndexedTxModelCStruct.class);
  }
}
