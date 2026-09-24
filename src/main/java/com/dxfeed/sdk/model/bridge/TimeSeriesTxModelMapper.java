package com.dxfeed.sdk.model.bridge;

import com.dxfeed.sdk.mappers.JavaObjectHandlerMapper;
import org.graalvm.nativeimage.c.struct.SizeOf;

public class TimeSeriesTxModelMapper extends JavaObjectHandlerMapper {
  @Override
  protected int getSizeJavaObjectHandler() {
    return SizeOf.get(TimeSeriesTxModelCStruct.class);
  }
}
