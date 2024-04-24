package com.dxfeed.sdk.schedule;

import org.graalvm.nativeimage.IsolateThread;
import org.graalvm.nativeimage.c.CContext;
import org.graalvm.nativeimage.c.function.CFunctionPointer;
import org.graalvm.nativeimage.c.function.InvokeCFunctionPointer;
import org.graalvm.nativeimage.c.type.CTypedef;

@CContext(Directives.class)
@CTypedef(name = "dxfg_session_filter_function")
interface DxfgSessionFilterFunction extends CFunctionPointer {

  @InvokeCFunctionPointer
  int invoke(
      final IsolateThread thread,
      final DxfgSession dxfgSession
  );
}
