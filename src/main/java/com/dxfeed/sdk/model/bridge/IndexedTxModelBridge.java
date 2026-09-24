package com.dxfeed.sdk.model.bridge;

import com.dxfeed.api.experimental.model.IndexedTxModel;
import com.dxfeed.event.IndexedEvent;
import com.dxfeed.sdk.NativeUtils;
import com.dxfeed.sdk.events.DxfgEventClazz;
import com.dxfeed.sdk.events.DxfgIndexedEventSourceList;
import com.dxfeed.sdk.exception.ExceptionHandlerReturnMinusOne;
import com.dxfeed.sdk.exception.ExceptionHandlerReturnNullWord;
import com.dxfeed.sdk.feed.DxfgFeed;
import com.dxfeed.sdk.javac.DxfgTimePeriodHandle;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.graalvm.nativeimage.IsolateThread;
import org.graalvm.nativeimage.c.CContext;
import org.graalvm.nativeimage.c.function.CEntryPoint;

@CContext(IndexedTxModelBridge.Directives.class)
public class IndexedTxModelBridge {
  @CEntryPoint(
      name = "dxfg_IndexedTxModel_newBuilder",
      exceptionHandler = ExceptionHandlerReturnNullWord.class
  )
  public static IndexedTxModelBuilderCStruct dxfg_IndexedTxModel_newBuilder(
      IsolateThread ignoredThread, DxfgEventClazz eventType) {
    IndexedTxModel.Builder result = (IndexedTxModel.Builder) IndexedTxModel.newBuilder((Class<IndexedEvent>) (Object) eventType.clazz);
    return IndexedTxModelBuilderUtils.MAPPER.toNative(result);
  }

  @CEntryPoint(
      name = "dxfg_IndexedTxModel_getSources",
      exceptionHandler = ExceptionHandlerReturnNullWord.class
  )
  public static DxfgIndexedEventSourceList dxfg_IndexedTxModel_getSources(
      IsolateThread ignoredThread, IndexedTxModelCStruct source) {
    Set result = (Set) IndexedTxModelUtils.MAPPER.toJava(source).getSources();
    return NativeUtils.MAPPER_INDEXED_EVENT_SOURCES.toNativeList(result);
  }

  @CEntryPoint(
      name = "dxfg_IndexedTxModel_setSources",
      exceptionHandler = ExceptionHandlerReturnMinusOne.class
  )
  public static int dxfg_IndexedTxModel_setSources(IsolateThread ignoredThread,
      IndexedTxModelCStruct source, DxfgIndexedEventSourceList sources) {
    IndexedTxModelUtils.MAPPER.toJava(source).setSources(new HashSet<>(NativeUtils.MAPPER_INDEXED_EVENT_SOURCES.toJavaList(sources)));
    return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
  }

  @CEntryPoint(
      name = "dxfg_IndexedTxModel_isBatchProcessing",
      exceptionHandler = ExceptionHandlerReturnMinusOne.class
  )
  public static int dxfg_IndexedTxModel_isBatchProcessing(IsolateThread ignoredThread,
      IndexedTxModelCStruct source) {
    boolean result = (boolean) IndexedTxModelUtils.MAPPER.toJava(source).isBatchProcessing();
    return result ? 1 : 0;
  }

  @CEntryPoint(
      name = "dxfg_IndexedTxModel_isSnapshotProcessing",
      exceptionHandler = ExceptionHandlerReturnMinusOne.class
  )
  public static int dxfg_IndexedTxModel_isSnapshotProcessing(IsolateThread ignoredThread,
      IndexedTxModelCStruct source) {
    boolean result = (boolean) IndexedTxModelUtils.MAPPER.toJava(source).isSnapshotProcessing();
    return result ? 1 : 0;
  }

  @CEntryPoint(
      name = "dxfg_IndexedTxModel_attach",
      exceptionHandler = ExceptionHandlerReturnMinusOne.class
  )
  public static int dxfg_IndexedTxModel_attach(IsolateThread ignoredThread,
      IndexedTxModelCStruct source, DxfgFeed feed) {
    IndexedTxModelUtils.MAPPER.toJava(source).attach(NativeUtils.MAPPER_FEED.toJava(feed));
    return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
  }

  @CEntryPoint(
      name = "dxfg_IndexedTxModel_detach",
      exceptionHandler = ExceptionHandlerReturnMinusOne.class
  )
  public static int dxfg_IndexedTxModel_detach(IsolateThread ignoredThread,
      IndexedTxModelCStruct source, DxfgFeed feed) {
    IndexedTxModelUtils.MAPPER.toJava(source).detach(NativeUtils.MAPPER_FEED.toJava(feed));
    return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
  }

  @CEntryPoint(
      name = "dxfg_IndexedTxModel_setAggregationPeriod",
      exceptionHandler = ExceptionHandlerReturnMinusOne.class
  )
  public static int dxfg_IndexedTxModel_setAggregationPeriod(IsolateThread ignoredThread,
      IndexedTxModelCStruct source, DxfgTimePeriodHandle aggregationPeriod) {
    IndexedTxModelUtils.MAPPER.toJava(source).setAggregationPeriod(NativeUtils.MAPPER_TIME_PERIOD.toJava(aggregationPeriod));
    return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
  }

  @CEntryPoint(
      name = "dxfg_IndexedTxModel_close",
      exceptionHandler = ExceptionHandlerReturnMinusOne.class
  )
  public static int dxfg_IndexedTxModel_close(IsolateThread ignoredThread,
      IndexedTxModelCStruct source) {
    IndexedTxModelUtils.MAPPER.toJava(source).close();
    return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
  }

  public static class Directives implements CContext.Directives {
    @Override
    public List<String> getHeaderFiles() {
      return Collections.singletonList("\"" + Path.of(System.getProperty("project.path"), "src/main/c/api/dxfg_event_model.h").toAbsolutePath() + "\"");
    }
  }
}
