package com.dxfeed.sdk.model.bridge;

import com.dxfeed.sdk.mappers.JavaObjectHandlerMapper;
import org.graalvm.nativeimage.c.struct.SizeOf;

public class IndexedTxModelMapper extends JavaObjectHandlerMapper {
  @Override
  protected int getSizeJavaObjectHandler() {
    return SizeOf.get(IndexedTxModelCStruct.class);
  }
}
