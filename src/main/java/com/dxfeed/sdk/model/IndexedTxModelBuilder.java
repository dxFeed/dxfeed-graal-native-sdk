// Copyright (c) 2025 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

package com.dxfeed.sdk.model;

import com.dxfeed.api.DXFeed;
import com.dxfeed.api.experimental.model.IndexedTxModel.Builder;
import com.dxfeed.bridge.annotations.Ignore;
import com.dxfeed.bridge.annotations.ObjectHandler;
import com.dxfeed.bridge.annotations.Parameter;
import com.dxfeed.event.IndexedEvent;
import com.dxfeed.event.IndexedEventSource;
import com.dxfeed.sdk.NativeUtils;
import com.dxfeed.sdk.events.DxfgIndexedEventSourceList;
import com.dxfeed.sdk.feed.DxfgFeed;
import com.dxfeed.sdk.javac.DxfgExecutor;
import com.dxfeed.sdk.symbol.DxfgSymbol;
import java.util.Collection;
import java.util.concurrent.Executor;

@ObjectHandler(
        value = com.dxfeed.api.experimental.model.IndexedTxModel.Builder.class,
        cStruct = "dxfg_indexed_tx_model_builder_t",
        cHeader = "src/main/c/api/dxfg_event_model.h"
)
public interface IndexedTxModelBuilder {

    @Ignore
    public Builder<IndexedEvent<?>> withSources(IndexedEventSource... sources);

    @Parameter(value = DxfgIndexedEventSourceList.class, mapperParameters = NativeUtils.class, mapperCodeTemplateToJava = "$T.MAPPER_INDEXED_EVENT_SOURCES.toJavaList($N)", cTypeName = "dxfg_indexed_event_source_list*")
    public Builder<IndexedEvent<?>> withSources(Collection<? extends IndexedEventSource> sources);

    @Parameter(value = DxfgFeed.class, mapperParameters = NativeUtils.class, mapperCodeTemplateToJava = "$T.MAPPER_FEED.toJava($N)", cTypeName = "dxfg_feed_t*")
    public void withFeed(DXFeed feed);

    @Parameter(value = DxfgSymbol.class, mapperParameters = NativeUtils.class, mapperCodeTemplateToJava = "$T.MAPPER_SYMBOL.toJava($N)", cTypeName = "dxfg_symbol_t*")
    public void withSymbol(Object symbol);

    @Parameter(value = DxfgExecutor.class, mapperParameters = NativeUtils.class, mapperCodeTemplateToJava = "$T.MAPPER_EXECUTOR.toJava($N)", cTypeName = "dxfg_executor_t*")
    public void withExecutor(Executor executor);
}
