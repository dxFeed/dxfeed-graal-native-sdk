package com.dxfeed.sdk.model.bridge;

import com.dxfeed.api.experimental.model.TxModelListener;
import com.dxfeed.event.EventType;
import com.dxfeed.sdk.NativeUtils;
import com.dxfeed.sdk.events.DxfgEventTypeListPointer;
import com.dxfeed.sdk.exception.ExceptionHandlerReturnNullWord;
import com.dxfeed.sdk.source.DxfgIndexedEventSourcePointer;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import org.graalvm.nativeimage.CurrentIsolate;
import org.graalvm.nativeimage.IsolateThread;
import org.graalvm.nativeimage.c.CContext;
import org.graalvm.nativeimage.c.function.CEntryPoint;
import org.graalvm.nativeimage.c.type.VoidPointer;

@CContext(TxModelListenerBridge.Directives.class)
public class TxModelListenerBridge {
  @CEntryPoint(
      name = "dxfg_TxModelListener_new",
      exceptionHandler = ExceptionHandlerReturnNullWord.class
  )
  public static TxModelListenerCStruct dxfg_TxModelListener_new(IsolateThread ignoreThread,
      DxfgTxModelListenerFunctionEventsReceived functionEventsReceived, VoidPointer userData) {
    return TxModelListenerUtils.MAPPER.toNative(
      new TxModelListener() {

        @Override
        public void eventsReceived(final com.dxfeed.event.IndexedEventSource source, final List events, final boolean isSnapshot) {
          DxfgIndexedEventSourcePointer sourceNative = NativeUtils.MAPPER_INDEXED_EVENT_SOURCE.toNative(source);
          DxfgEventTypeListPointer eventsNative = NativeUtils.MAPPER_EVENTS.toNativeList((Collection<? extends EventType<?>>) events);
          boolean isSnapshotNative = isSnapshot;
          functionEventsReceived.invoke(
              CurrentIsolate.getCurrentThread(),
              sourceNative,
              eventsNative,
              isSnapshotNative,
              userData
          );
          NativeUtils.MAPPER_INDEXED_EVENT_SOURCE.release(sourceNative);
          NativeUtils.MAPPER_EVENTS.release(eventsNative);
        }

    });
  }

  public static class Directives implements CContext.Directives {
    @Override
    public List<String> getHeaderFiles() {
      return Collections.singletonList("\"" + Path.of(System.getProperty("project.path"), "src/main/c/api/dxfg_event_model.h").toAbsolutePath() + "\"");
    }
  }
}
