package com.dxfeed.sdk.model;

import com.dxfeed.api.DXFeed;
import com.dxfeed.api.experimental.model.TimeSeriesTxModel.Builder;
import com.dxfeed.bridge.annotations.ObjectHandler;
import com.dxfeed.bridge.annotations.Parameter;
import com.dxfeed.event.TimeSeriesEvent;
import com.dxfeed.sdk.NativeUtils;
import com.dxfeed.sdk.events.DxfgEventClazz;
import com.dxfeed.sdk.feed.DxfgFeed;

@ObjectHandler(
    value = com.dxfeed.api.experimental.model.TimeSeriesTxModel.class,
    cStruct = "dxfg_time_series_tx_model_t",
    cHeader = "src/main/c/api/dxfg_event_model.h"
)
public interface TimeSeriesTxModel {

  @Parameter(value = DxfgEventClazz.class, mapperParameters = {TimeSeriesEvent.class,
      Object.class}, mapperCodeTemplateToJava = "(Class<$T>) ($T) $N.clazz", cTypeName = "dxfg_event_clazz_t")
  public static <E extends TimeSeriesEvent<?>> Builder<E> newBuilder(Class<E> eventType) {
    return null;
  }

  @Parameter(value = DxfgFeed.class, mapperParameters = NativeUtils.class, mapperCodeTemplateToJava = "$T.MAPPER_FEED.toJava($N)", cTypeName = "dxfg_feed_t*")
  public void attach(DXFeed feed);

  @Parameter(value = DxfgFeed.class, mapperParameters = NativeUtils.class, mapperCodeTemplateToJava = "$T.MAPPER_FEED.toJava($N)", cTypeName = "dxfg_feed_t*")
  public void detach(DXFeed feed);
}
