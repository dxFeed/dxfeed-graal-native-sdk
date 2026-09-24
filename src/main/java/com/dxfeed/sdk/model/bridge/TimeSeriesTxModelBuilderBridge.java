package com.dxfeed.sdk.model.bridge;

import com.dxfeed.api.experimental.model.TimeSeriesTxModel;
import com.dxfeed.sdk.NativeUtils;
import com.dxfeed.sdk.exception.ExceptionHandlerReturnNullWord;
import com.dxfeed.sdk.feed.DxfgFeed;
import com.dxfeed.sdk.javac.DxfgExecutorHandle;
import com.dxfeed.sdk.javac.DxfgTimePeriodHandle;
import com.dxfeed.sdk.model.DxfgTimeSeriesTxModelSortOrder;
import com.dxfeed.sdk.symbol.DxfgSymbol;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import org.graalvm.nativeimage.IsolateThread;
import org.graalvm.nativeimage.c.CContext;
import org.graalvm.nativeimage.c.function.CEntryPoint;
import org.graalvm.nativeimage.c.type.VoidPointer;

@CContext(TimeSeriesTxModelBuilderBridge.Directives.class)
public class TimeSeriesTxModelBuilderBridge {
  @CEntryPoint(
      name = "dxfg_TimeSeriesTxModel_Builder_withFromTime",
      exceptionHandler = ExceptionHandlerReturnNullWord.class
  )
  public static TimeSeriesTxModelBuilderCStruct dxfg_TimeSeriesTxModel_Builder_withFromTime(
      IsolateThread ignoredThread, TimeSeriesTxModelBuilderCStruct source, long fromTime) {
    TimeSeriesTxModel.Builder result = (TimeSeriesTxModel.Builder) TimeSeriesTxModelBuilderUtils.MAPPER.toJava(source).withFromTime(fromTime);
    return TimeSeriesTxModelBuilderUtils.MAPPER.toNative(result);
  }

  @CEntryPoint(
      name = "dxfg_TimeSeriesTxModel_Builder_withSorting",
      exceptionHandler = ExceptionHandlerReturnNullWord.class
  )
  public static TimeSeriesTxModelBuilderCStruct dxfg_TimeSeriesTxModel_Builder_withSorting(
      IsolateThread ignoredThread, TimeSeriesTxModelBuilderCStruct source,
      DxfgTimeSeriesTxModelSortOrder sortOrder) {
    TimeSeriesTxModel.Builder result = (TimeSeriesTxModel.Builder) TimeSeriesTxModelBuilderUtils.MAPPER.toJava(source).withSorting(sortOrder.sortOrder);
    return TimeSeriesTxModelBuilderUtils.MAPPER.toNative(result);
  }

  @CEntryPoint(
      name = "dxfg_TimeSeriesTxModel_Builder_ignoreRemoveEvents",
      exceptionHandler = ExceptionHandlerReturnNullWord.class
  )
  public static TimeSeriesTxModelBuilderCStruct dxfg_TimeSeriesTxModel_Builder_ignoreRemoveEvents(
      IsolateThread ignoredThread, TimeSeriesTxModelBuilderCStruct source,
      boolean ignoreRemoveEvents) {
    TimeSeriesTxModel.Builder result = (TimeSeriesTxModel.Builder) TimeSeriesTxModelBuilderUtils.MAPPER.toJava(source).ignoreRemoveEvents(ignoreRemoveEvents);
    return TimeSeriesTxModelBuilderUtils.MAPPER.toNative(result);
  }

  @CEntryPoint(
      name = "dxfg_TimeSeriesTxModel_Builder_ignoreEventsFromPast",
      exceptionHandler = ExceptionHandlerReturnNullWord.class
  )
  public static TimeSeriesTxModelBuilderCStruct dxfg_TimeSeriesTxModel_Builder_ignoreEventsFromPast(
      IsolateThread ignoredThread, TimeSeriesTxModelBuilderCStruct source,
      boolean ignoreEventFromPast) {
    TimeSeriesTxModel.Builder result = (TimeSeriesTxModel.Builder) TimeSeriesTxModelBuilderUtils.MAPPER.toJava(source).ignoreEventsFromPast(ignoreEventFromPast);
    return TimeSeriesTxModelBuilderUtils.MAPPER.toNative(result);
  }

  @CEntryPoint(
      name = "dxfg_TimeSeriesTxModel_Builder_withConfirmationTick",
      exceptionHandler = ExceptionHandlerReturnNullWord.class
  )
  public static TimeSeriesTxModelBuilderCStruct dxfg_TimeSeriesTxModel_Builder_withConfirmationTick(
      IsolateThread ignoredThread, TimeSeriesTxModelBuilderCStruct source,
      boolean confirmationTick) {
    TimeSeriesTxModel.Builder result = (TimeSeriesTxModel.Builder) TimeSeriesTxModelBuilderUtils.MAPPER.toJava(source).withConfirmationTick(confirmationTick);
    return TimeSeriesTxModelBuilderUtils.MAPPER.toNative(result);
  }

  @CEntryPoint(
      name = "dxfg_TimeSeriesTxModel_Builder_withEventPeek",
      exceptionHandler = ExceptionHandlerReturnNullWord.class
  )
  public static TimeSeriesTxModelBuilderCStruct dxfg_TimeSeriesTxModel_Builder_withEventPeek(
      IsolateThread ignoredThread, TimeSeriesTxModelBuilderCStruct source, VoidPointer eventPeek) {
    TimeSeriesTxModel.Builder result = (TimeSeriesTxModel.Builder) TimeSeriesTxModelBuilderUtils.MAPPER.toJava(source).withEventPeek(null /*eventPeek*/);
    return TimeSeriesTxModelBuilderUtils.MAPPER.toNative(result);
  }

  @CEntryPoint(
      name = "dxfg_TimeSeriesTxModel_Builder_build",
      exceptionHandler = ExceptionHandlerReturnNullWord.class
  )
  public static TimeSeriesTxModelCStruct dxfg_TimeSeriesTxModel_Builder_build(
      IsolateThread ignoredThread, TimeSeriesTxModelBuilderCStruct source) {
    TimeSeriesTxModel result = (TimeSeriesTxModel) TimeSeriesTxModelBuilderUtils.MAPPER.toJava(source).build();
    return TimeSeriesTxModelUtils.MAPPER.toNative(result);
  }

  @CEntryPoint(
      name = "dxfg_TimeSeriesTxModel_Builder_withBatchProcessing",
      exceptionHandler = ExceptionHandlerReturnNullWord.class
  )
  public static TimeSeriesTxModelBuilderCStruct dxfg_TimeSeriesTxModel_Builder_withBatchProcessing(
      IsolateThread ignoredThread, TimeSeriesTxModelBuilderCStruct source,
      boolean isBatchProcessing) {
    TimeSeriesTxModel.Builder result = (TimeSeriesTxModel.Builder) TimeSeriesTxModelBuilderUtils.MAPPER.toJava(source).withBatchProcessing(isBatchProcessing);
    return TimeSeriesTxModelBuilderUtils.MAPPER.toNative(result);
  }

  @CEntryPoint(
      name = "dxfg_TimeSeriesTxModel_Builder_withSnapshotProcessing",
      exceptionHandler = ExceptionHandlerReturnNullWord.class
  )
  public static TimeSeriesTxModelBuilderCStruct dxfg_TimeSeriesTxModel_Builder_withSnapshotProcessing(
      IsolateThread ignoredThread, TimeSeriesTxModelBuilderCStruct source,
      boolean isSnapshotProcessing) {
    TimeSeriesTxModel.Builder result = (TimeSeriesTxModel.Builder) TimeSeriesTxModelBuilderUtils.MAPPER.toJava(source).withSnapshotProcessing(isSnapshotProcessing);
    return TimeSeriesTxModelBuilderUtils.MAPPER.toNative(result);
  }

  @CEntryPoint(
      name = "dxfg_TimeSeriesTxModel_Builder_withFeed",
      exceptionHandler = ExceptionHandlerReturnNullWord.class
  )
  public static TimeSeriesTxModelBuilderCStruct dxfg_TimeSeriesTxModel_Builder_withFeed(
      IsolateThread ignoredThread, TimeSeriesTxModelBuilderCStruct source, DxfgFeed feed) {
    TimeSeriesTxModel.Builder result = (TimeSeriesTxModel.Builder) TimeSeriesTxModelBuilderUtils.MAPPER.toJava(source).withFeed(NativeUtils.MAPPER_FEED.toJava(feed));
    return TimeSeriesTxModelBuilderUtils.MAPPER.toNative(result);
  }

  @CEntryPoint(
      name = "dxfg_TimeSeriesTxModel_Builder_withAggregationPeriod",
      exceptionHandler = ExceptionHandlerReturnNullWord.class
  )
  public static TimeSeriesTxModelBuilderCStruct dxfg_TimeSeriesTxModel_Builder_withAggregationPeriod(
      IsolateThread ignoredThread, TimeSeriesTxModelBuilderCStruct source,
      DxfgTimePeriodHandle aggregationPeriod) {
    TimeSeriesTxModel.Builder result = (TimeSeriesTxModel.Builder) TimeSeriesTxModelBuilderUtils.MAPPER.toJava(source).withAggregationPeriod(NativeUtils.MAPPER_TIME_PERIOD.toJava(aggregationPeriod));
    return TimeSeriesTxModelBuilderUtils.MAPPER.toNative(result);
  }

  @CEntryPoint(
      name = "dxfg_TimeSeriesTxModel_Builder_withSymbol",
      exceptionHandler = ExceptionHandlerReturnNullWord.class
  )
  public static TimeSeriesTxModelBuilderCStruct dxfg_TimeSeriesTxModel_Builder_withSymbol(
      IsolateThread ignoredThread, TimeSeriesTxModelBuilderCStruct source, DxfgSymbol symbol) {
    TimeSeriesTxModel.Builder result = (TimeSeriesTxModel.Builder) TimeSeriesTxModelBuilderUtils.MAPPER.toJava(source).withSymbol(NativeUtils.MAPPER_SYMBOL.toJava(symbol));
    return TimeSeriesTxModelBuilderUtils.MAPPER.toNative(result);
  }

  @CEntryPoint(
      name = "dxfg_TimeSeriesTxModel_Builder_withListener",
      exceptionHandler = ExceptionHandlerReturnNullWord.class
  )
  public static TimeSeriesTxModelBuilderCStruct dxfg_TimeSeriesTxModel_Builder_withListener(
      IsolateThread ignoredThread, TimeSeriesTxModelBuilderCStruct source,
      TxModelListenerCStruct listener) {
    TimeSeriesTxModel.Builder result = (TimeSeriesTxModel.Builder) TimeSeriesTxModelBuilderUtils.MAPPER.toJava(source).withListener(TxModelListenerUtils.MAPPER.toJava(listener));
    return TimeSeriesTxModelBuilderUtils.MAPPER.toNative(result);
  }

  @CEntryPoint(
      name = "dxfg_TimeSeriesTxModel_Builder_withExecutor",
      exceptionHandler = ExceptionHandlerReturnNullWord.class
  )
  public static TimeSeriesTxModelBuilderCStruct dxfg_TimeSeriesTxModel_Builder_withExecutor(
      IsolateThread ignoredThread, TimeSeriesTxModelBuilderCStruct source,
      DxfgExecutorHandle executor) {
    TimeSeriesTxModel.Builder result = (TimeSeriesTxModel.Builder) TimeSeriesTxModelBuilderUtils.MAPPER.toJava(source).withExecutor(NativeUtils.MAPPER_EXECUTOR.toJava(executor));
    return TimeSeriesTxModelBuilderUtils.MAPPER.toNative(result);
  }

  public static class Directives implements CContext.Directives {
    @Override
    public List<String> getHeaderFiles() {
      return Collections.singletonList("\"" + Path.of(System.getProperty("project.path"), "src/main/c/api/dxfg_event_model.h").toAbsolutePath() + "\"");
    }
  }
}
