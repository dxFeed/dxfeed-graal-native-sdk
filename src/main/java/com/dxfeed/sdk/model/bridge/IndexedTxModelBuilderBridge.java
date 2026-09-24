package com.dxfeed.sdk.model.bridge;

import com.dxfeed.api.experimental.model.IndexedTxModel;
import com.dxfeed.sdk.NativeUtils;
import com.dxfeed.sdk.events.DxfgIndexedEventSourceList;
import com.dxfeed.sdk.exception.ExceptionHandlerReturnNullWord;
import com.dxfeed.sdk.feed.DxfgFeed;
import com.dxfeed.sdk.javac.DxfgExecutorHandle;
import com.dxfeed.sdk.javac.DxfgTimePeriodHandle;
import com.dxfeed.sdk.symbol.DxfgSymbol;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import org.graalvm.nativeimage.IsolateThread;
import org.graalvm.nativeimage.c.CContext;
import org.graalvm.nativeimage.c.function.CEntryPoint;

@CContext(IndexedTxModelBuilderBridge.Directives.class)
public class IndexedTxModelBuilderBridge {
  @CEntryPoint(
      name = "dxfg_IndexedTxModel_Builder_withSources",
      exceptionHandler = ExceptionHandlerReturnNullWord.class
  )
  public static IndexedTxModelBuilderCStruct dxfg_IndexedTxModel_Builder_withSources(
      IsolateThread ignoredThread, IndexedTxModelBuilderCStruct source,
      DxfgIndexedEventSourceList sources) {
    IndexedTxModel.Builder result = (IndexedTxModel.Builder) IndexedTxModelBuilderUtils.MAPPER.toJava(source).withSources(NativeUtils.MAPPER_INDEXED_EVENT_SOURCES.toJavaList(sources));
    return IndexedTxModelBuilderUtils.MAPPER.toNative(result);
  }

  @CEntryPoint(
      name = "dxfg_IndexedTxModel_Builder_build",
      exceptionHandler = ExceptionHandlerReturnNullWord.class
  )
  public static IndexedTxModelCStruct dxfg_IndexedTxModel_Builder_build(IsolateThread ignoredThread,
      IndexedTxModelBuilderCStruct source) {
    IndexedTxModel result = (IndexedTxModel) IndexedTxModelBuilderUtils.MAPPER.toJava(source).build();
    return IndexedTxModelUtils.MAPPER.toNative(result);
  }

  @CEntryPoint(
      name = "dxfg_IndexedTxModel_Builder_withBatchProcessing",
      exceptionHandler = ExceptionHandlerReturnNullWord.class
  )
  public static IndexedTxModelBuilderCStruct dxfg_IndexedTxModel_Builder_withBatchProcessing(
      IsolateThread ignoredThread, IndexedTxModelBuilderCStruct source, boolean isBatchProcessing) {
    IndexedTxModel.Builder result = (IndexedTxModel.Builder) IndexedTxModelBuilderUtils.MAPPER.toJava(source).withBatchProcessing(isBatchProcessing);
    return IndexedTxModelBuilderUtils.MAPPER.toNative(result);
  }

  @CEntryPoint(
      name = "dxfg_IndexedTxModel_Builder_withSnapshotProcessing",
      exceptionHandler = ExceptionHandlerReturnNullWord.class
  )
  public static IndexedTxModelBuilderCStruct dxfg_IndexedTxModel_Builder_withSnapshotProcessing(
      IsolateThread ignoredThread, IndexedTxModelBuilderCStruct source,
      boolean isSnapshotProcessing) {
    IndexedTxModel.Builder result = (IndexedTxModel.Builder) IndexedTxModelBuilderUtils.MAPPER.toJava(source).withSnapshotProcessing(isSnapshotProcessing);
    return IndexedTxModelBuilderUtils.MAPPER.toNative(result);
  }

  @CEntryPoint(
      name = "dxfg_IndexedTxModel_Builder_withFeed",
      exceptionHandler = ExceptionHandlerReturnNullWord.class
  )
  public static IndexedTxModelBuilderCStruct dxfg_IndexedTxModel_Builder_withFeed(
      IsolateThread ignoredThread, IndexedTxModelBuilderCStruct source, DxfgFeed feed) {
    IndexedTxModel.Builder result = (IndexedTxModel.Builder) IndexedTxModelBuilderUtils.MAPPER.toJava(source).withFeed(NativeUtils.MAPPER_FEED.toJava(feed));
    return IndexedTxModelBuilderUtils.MAPPER.toNative(result);
  }

  @CEntryPoint(
      name = "dxfg_IndexedTxModel_Builder_withAggregationPeriod",
      exceptionHandler = ExceptionHandlerReturnNullWord.class
  )
  public static IndexedTxModelBuilderCStruct dxfg_IndexedTxModel_Builder_withAggregationPeriod(
      IsolateThread ignoredThread, IndexedTxModelBuilderCStruct source,
      DxfgTimePeriodHandle aggregationPeriod) {
    IndexedTxModel.Builder result = (IndexedTxModel.Builder) IndexedTxModelBuilderUtils.MAPPER.toJava(source).withAggregationPeriod(NativeUtils.MAPPER_TIME_PERIOD.toJava(aggregationPeriod));
    return IndexedTxModelBuilderUtils.MAPPER.toNative(result);
  }

  @CEntryPoint(
      name = "dxfg_IndexedTxModel_Builder_withSymbol",
      exceptionHandler = ExceptionHandlerReturnNullWord.class
  )
  public static IndexedTxModelBuilderCStruct dxfg_IndexedTxModel_Builder_withSymbol(
      IsolateThread ignoredThread, IndexedTxModelBuilderCStruct source, DxfgSymbol symbol) {
    IndexedTxModel.Builder result = (IndexedTxModel.Builder) IndexedTxModelBuilderUtils.MAPPER.toJava(source).withSymbol(NativeUtils.MAPPER_SYMBOL.toJava(symbol));
    return IndexedTxModelBuilderUtils.MAPPER.toNative(result);
  }

  @CEntryPoint(
      name = "dxfg_IndexedTxModel_Builder_withListener",
      exceptionHandler = ExceptionHandlerReturnNullWord.class
  )
  public static IndexedTxModelBuilderCStruct dxfg_IndexedTxModel_Builder_withListener(
      IsolateThread ignoredThread, IndexedTxModelBuilderCStruct source,
      TxModelListenerCStruct listener) {
    IndexedTxModel.Builder result = (IndexedTxModel.Builder) IndexedTxModelBuilderUtils.MAPPER.toJava(source).withListener(TxModelListenerUtils.MAPPER.toJava(listener));
    return IndexedTxModelBuilderUtils.MAPPER.toNative(result);
  }

  @CEntryPoint(
      name = "dxfg_IndexedTxModel_Builder_withExecutor",
      exceptionHandler = ExceptionHandlerReturnNullWord.class
  )
  public static IndexedTxModelBuilderCStruct dxfg_IndexedTxModel_Builder_withExecutor(
      IsolateThread ignoredThread, IndexedTxModelBuilderCStruct source,
      DxfgExecutorHandle executor) {
    IndexedTxModel.Builder result = (IndexedTxModel.Builder) IndexedTxModelBuilderUtils.MAPPER.toJava(source).withExecutor(NativeUtils.MAPPER_EXECUTOR.toJava(executor));
    return IndexedTxModelBuilderUtils.MAPPER.toNative(result);
  }

  public static class Directives implements CContext.Directives {
    @Override
    public List<String> getHeaderFiles() {
      return Collections.singletonList("\"" + Path.of(System.getProperty("project.path"), "src/main/c/api/dxfg_event_model.h").toAbsolutePath() + "\"");
    }
  }
}
