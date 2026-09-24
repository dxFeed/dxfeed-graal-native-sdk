package com.dxfeed.sdk.model.bridge;

import com.dxfeed.api.experimental.model.TimeSeriesTxModel;
import com.dxfeed.sdk.mappers.JavaObjectHandlerMapper;

public final class TimeSeriesTxModelBuilderUtils {
  public static final JavaObjectHandlerMapper<TimeSeriesTxModel.Builder, TimeSeriesTxModelBuilderCStruct> MAPPER = new TimeSeriesTxModelBuilderMapper();
}
