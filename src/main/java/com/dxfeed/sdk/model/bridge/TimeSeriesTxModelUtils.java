package com.dxfeed.sdk.model.bridge;

import com.dxfeed.api.experimental.model.TimeSeriesTxModel;
import com.dxfeed.sdk.mappers.JavaObjectHandlerMapper;

public final class TimeSeriesTxModelUtils {
  public static final JavaObjectHandlerMapper<TimeSeriesTxModel, TimeSeriesTxModelCStruct> MAPPER = new TimeSeriesTxModelMapper();
}
