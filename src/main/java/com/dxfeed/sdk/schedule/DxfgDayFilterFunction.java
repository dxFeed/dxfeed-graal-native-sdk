package com.dxfeed.sdk.schedule;

import com.oracle.svm.core.c.CTypedef;
import org.graalvm.nativeimage.IsolateThread;
import org.graalvm.nativeimage.c.CContext;
import org.graalvm.nativeimage.c.function.CFunctionPointer;
import org.graalvm.nativeimage.c.function.InvokeCFunctionPointer;

@CContext(Directives.class)
@CTypedef(name = "dxfg_day_filter_function")
interface DxfgDayFilterFunction extends CFunctionPointer {

  @InvokeCFunctionPointer
  int invoke(
      final IsolateThread thread,
      final DxfgDay dxfgDay
  );
}
