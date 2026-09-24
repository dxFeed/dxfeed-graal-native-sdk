package com.dxfeed.sdk.model.bridge;

import com.dxfeed.api.experimental.model.TimeSeriesTxModel;
import com.dxfeed.event.TimeSeriesEvent;
import com.dxfeed.sdk.NativeUtils;
import com.dxfeed.sdk.events.DxfgEventClazz;
import com.dxfeed.sdk.exception.ExceptionHandlerReturnMinusOne;
import com.dxfeed.sdk.exception.ExceptionHandlerReturnNullWord;
import com.dxfeed.sdk.feed.DxfgFeed;
import com.dxfeed.sdk.javac.DxfgTimePeriodHandle;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import org.graalvm.nativeimage.IsolateThread;
import org.graalvm.nativeimage.c.CContext;
import org.graalvm.nativeimage.c.function.CEntryPoint;

@CContext(TimeSeriesTxModelBridge.Directives.class)
public class TimeSeriesTxModelBridge {
  @CEntryPoint(
      name = "dxfg_TimeSeriesTxModel_newBuilder",
      exceptionHandler = ExceptionHandlerReturnNullWord.class
  )
  public static TimeSeriesTxModelBuilderCStruct dxfg_TimeSeriesTxModel_newBuilder(
      IsolateThread ignoredThread, DxfgEventClazz eventType) {
    TimeSeriesTxModel.Builder result = (TimeSeriesTxModel.Builder) TimeSeriesTxModel.newBuilder((Class<TimeSeriesEvent>) (Object) eventType.clazz);
    return TimeSeriesTxModelBuilderUtils.MAPPER.toNative(result);
  }

  @CEntryPoint(
      name = "dxfg_TimeSeriesTxModel_getFromTime",
      exceptionHandler = ExceptionHandlerReturnMinusOne.class
  )
  public static long dxfg_TimeSeriesTxModel_getFromTime(IsolateThread ignoredThread,
      TimeSeriesTxModelCStruct source) {
    long result = (long) TimeSeriesTxModelUtils.MAPPER.toJava(source).getFromTime();
    return result;
  }

  @CEntryPoint(
      name = "dxfg_TimeSeriesTxModel_setFromTime",
      exceptionHandler = ExceptionHandlerReturnMinusOne.class
  )
  public static int dxfg_TimeSeriesTxModel_setFromTime(IsolateThread ignoredThread,
      TimeSeriesTxModelCStruct source, long fromTime) {
    TimeSeriesTxModelUtils.MAPPER.toJava(source).setFromTime(fromTime);
    return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
  }

  @CEntryPoint(
      name = "dxfg_TimeSeriesTxModel_isBatchProcessing",
      exceptionHandler = ExceptionHandlerReturnMinusOne.class
  )
  public static int dxfg_TimeSeriesTxModel_isBatchProcessing(IsolateThread ignoredThread,
      TimeSeriesTxModelCStruct source) {
    boolean result = (boolean) TimeSeriesTxModelUtils.MAPPER.toJava(source).isBatchProcessing();
    return result ? 1 : 0;
  }

  @CEntryPoint(
      name = "dxfg_TimeSeriesTxModel_isSnapshotProcessing",
      exceptionHandler = ExceptionHandlerReturnMinusOne.class
  )
  public static int dxfg_TimeSeriesTxModel_isSnapshotProcessing(IsolateThread ignoredThread,
      TimeSeriesTxModelCStruct source) {
    boolean result = (boolean) TimeSeriesTxModelUtils.MAPPER.toJava(source).isSnapshotProcessing();
    return result ? 1 : 0;
  }

  @CEntryPoint(
      name = "dxfg_TimeSeriesTxModel_attach",
      exceptionHandler = ExceptionHandlerReturnMinusOne.class
  )
  public static int dxfg_TimeSeriesTxModel_attach(IsolateThread ignoredThread,
      TimeSeriesTxModelCStruct source, DxfgFeed feed) {
    TimeSeriesTxModelUtils.MAPPER.toJava(source).attach(NativeUtils.MAPPER_FEED.toJava(feed));
    return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
  }

  @CEntryPoint(
      name = "dxfg_TimeSeriesTxModel_detach",
      exceptionHandler = ExceptionHandlerReturnMinusOne.class
  )
  public static int dxfg_TimeSeriesTxModel_detach(IsolateThread ignoredThread,
      TimeSeriesTxModelCStruct source, DxfgFeed feed) {
    TimeSeriesTxModelUtils.MAPPER.toJava(source).detach(NativeUtils.MAPPER_FEED.toJava(feed));
    return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
  }

  @CEntryPoint(
      name = "dxfg_TimeSeriesTxModel_setAggregationPeriod",
      exceptionHandler = ExceptionHandlerReturnMinusOne.class
  )
  public static int dxfg_TimeSeriesTxModel_setAggregationPeriod(IsolateThread ignoredThread,
      TimeSeriesTxModelCStruct source, DxfgTimePeriodHandle aggregationPeriod) {
    TimeSeriesTxModelUtils.MAPPER.toJava(source).setAggregationPeriod(NativeUtils.MAPPER_TIME_PERIOD.toJava(aggregationPeriod));
    return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
  }

  @CEntryPoint(
      name = "dxfg_TimeSeriesTxModel_close",
      exceptionHandler = ExceptionHandlerReturnMinusOne.class
  )
  public static int dxfg_TimeSeriesTxModel_close(IsolateThread ignoredThread,
      TimeSeriesTxModelCStruct source) {
    TimeSeriesTxModelUtils.MAPPER.toJava(source).close();
    return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
  }

  public static class Directives implements CContext.Directives {
    @Override
    public List<String> getHeaderFiles() {
      return Collections.singletonList("\"" + Path.of(System.getProperty("project.path"), "src/main/c/api/dxfg_event_model.h").toAbsolutePath() + "\"");
    }
  }
}
