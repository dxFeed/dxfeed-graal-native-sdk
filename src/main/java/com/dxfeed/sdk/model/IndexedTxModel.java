package com.dxfeed.sdk.model;

import com.dxfeed.api.DXFeed;
import com.dxfeed.api.experimental.model.IndexedTxModel.Builder;
import com.dxfeed.bridge.annotations.Ignore;
import com.dxfeed.bridge.annotations.ObjectHandler;
import com.dxfeed.bridge.annotations.Parameter;
import com.dxfeed.bridge.annotations.ReturnType;
import com.dxfeed.event.IndexedEvent;
import com.dxfeed.event.IndexedEventSource;
import com.dxfeed.sdk.NativeUtils;
import com.dxfeed.sdk.events.DxfgEventClazz;
import com.dxfeed.sdk.events.DxfgIndexedEventSourceList;
import com.dxfeed.sdk.feed.DxfgFeed;
import java.util.HashSet;
import java.util.Set;

@ObjectHandler(
    value = com.dxfeed.api.experimental.model.IndexedTxModel.class,
    cStruct = "dxfg_indexed_tx_model_t",
    cHeader = "src/main/c/api/dxfg_event_model.h"
)
public interface IndexedTxModel {

  @Parameter(value = DxfgEventClazz.class, mapperParameters = IndexedEvent.class, mapperCodeTemplateToJava = "(Class<$T>) (Object) $N.clazz", cTypeName = "dxfg_event_clazz_t")
  public static <E extends IndexedEvent<?>> Builder<E> newBuilder(Class<E> eventType) {
    return null;
  }

  @ReturnType(
      value = DxfgIndexedEventSourceList.class,
      mapperCodeTemplate = "$T.MAPPER_INDEXED_EVENT_SOURCES.toNativeList($N)",
      mapperParameters = NativeUtils.class,
      cTypeName = "dxfg_indexed_event_source_list*"
  )
  public Set<IndexedEventSource> getSources();

  @Ignore
  public void setSources(IndexedEventSource... sources);

  @Parameter(value = DxfgIndexedEventSourceList.class, mapperParameters = {HashSet.class ,NativeUtils.class}, mapperCodeTemplateToJava = "new $T<>($T.MAPPER_INDEXED_EVENT_SOURCES.toJavaList($N))", cTypeName = "dxfg_indexed_event_source_list*")
  public void setSources(Set<? extends IndexedEventSource> sources);

  @Parameter(value = DxfgFeed.class, mapperParameters = NativeUtils.class, mapperCodeTemplateToJava = "$T.MAPPER_FEED.toJava($N)", cTypeName = "dxfg_feed_t*")
  public void attach(DXFeed feed);

  @Parameter(value = DxfgFeed.class, mapperParameters = NativeUtils.class, mapperCodeTemplateToJava = "$T.MAPPER_FEED.toJava($N)", cTypeName = "dxfg_feed_t*")
  public void detach(DXFeed feed);
}
