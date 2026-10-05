package com.dxfeed.sdk.model.bridge;

import com.dxfeed.api.experimental.model.TimeSeriesTxModel;
import com.dxfeed.sdk.mappers.JavaObjectHandlerMapper;
import org.graalvm.nativeimage.c.struct.SizeOf;

public class TimeSeriesTxModelBuilderMapper extends JavaObjectHandlerMapper<TimeSeriesTxModel.Builder<?>, TimeSeriesTxModelBuilderCStruct> {
  @Override
  protected int getSizeJavaObjectHandler() {
    return SizeOf.get(TimeSeriesTxModelBuilderCStruct.class);
  }
}
