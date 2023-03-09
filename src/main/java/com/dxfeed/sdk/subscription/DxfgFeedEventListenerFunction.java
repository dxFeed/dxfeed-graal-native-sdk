package com.dxfeed.sdk.subscription;

import com.dxfeed.sdk.events.DxfgEventTypeList;
import com.oracle.svm.core.c.CTypedef;
import org.graalvm.nativeimage.IsolateThread;
import org.graalvm.nativeimage.c.CContext;
import org.graalvm.nativeimage.c.function.CFunctionPointer;
import org.graalvm.nativeimage.c.function.InvokeCFunctionPointer;
import org.graalvm.nativeimage.c.type.VoidPointer;

@CContext(Directives.class)
@CTypedef(name = "dxfg_feed_event_listener_function")
public interface DxfgFeedEventListenerFunction extends CFunctionPointer {

  @InvokeCFunctionPointer
  void invoke(
      final IsolateThread thread,
      final DxfgEventTypeList dxfgEventTypeList,
      final VoidPointer userData
  );
}
