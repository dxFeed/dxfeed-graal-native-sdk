package com.dxfeed.api.endpoint;

import com.oracle.svm.core.c.CTypedef;
import org.graalvm.nativeimage.IsolateThread;
import org.graalvm.nativeimage.c.CContext;
import org.graalvm.nativeimage.c.function.CFunctionPointer;
import org.graalvm.nativeimage.c.function.InvokeCFunctionPointer;
import org.graalvm.nativeimage.c.type.VoidPointer;

@CContext(Directives.class)
@CTypedef(name = "dxfg_endpoint_state_change_listener_func")
interface DxfgEndpointStateChangeListenerFunc extends CFunctionPointer {

  @InvokeCFunctionPointer
  void invoke(
      final IsolateThread thread,
      final DxfgEndpointState oldState,
      final DxfgEndpointState newState,
      final VoidPointer userData
  );
}
