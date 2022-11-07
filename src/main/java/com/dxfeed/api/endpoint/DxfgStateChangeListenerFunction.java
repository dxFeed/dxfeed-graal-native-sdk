package com.dxfeed.api.endpoint;

import org.graalvm.nativeimage.IsolateThread;
import org.graalvm.nativeimage.c.function.CFunctionPointer;
import org.graalvm.nativeimage.c.function.InvokeCFunctionPointer;

interface DxfgStateChangeListenerFunction extends CFunctionPointer {

  @InvokeCFunctionPointer
  void invoke(
      final IsolateThread thread,
      final DxfgEndpointState oldState,
      final DxfgEndpointState newState
  );
}
