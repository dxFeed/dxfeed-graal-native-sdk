package com.dxfeed.sdk.model.bridge;

import com.dxfeed.sdk.events.DxfgEventTypeListPointer;
import com.dxfeed.sdk.source.DxfgIndexedEventSourcePointer;
import org.graalvm.nativeimage.IsolateThread;
import org.graalvm.nativeimage.c.CContext;
import org.graalvm.nativeimage.c.function.CFunctionPointer;
import org.graalvm.nativeimage.c.function.InvokeCFunctionPointer;
import org.graalvm.nativeimage.c.type.CTypedef;
import org.graalvm.nativeimage.c.type.VoidPointer;

@CContext(TxModelListenerBridge.Directives.class)
@CTypedef(
    name = "dxfg_TxModelListener_function_eventsReceived"
)
public interface DxfgTxModelListenerFunctionEventsReceived extends CFunctionPointer {
  @InvokeCFunctionPointer
  int invoke(IsolateThread thread, DxfgIndexedEventSourcePointer source,
      DxfgEventTypeListPointer events, boolean isSnapshot, VoidPointer userData);
}
