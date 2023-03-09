package com.dxfeed.sdk.model;

import com.oracle.svm.core.c.CTypedef;
import org.graalvm.nativeimage.IsolateThread;
import org.graalvm.nativeimage.c.CContext;
import org.graalvm.nativeimage.c.function.CFunctionPointer;
import org.graalvm.nativeimage.c.function.InvokeCFunctionPointer;
import org.graalvm.nativeimage.c.type.VoidPointer;

@CContext(Directives.class)
@CTypedef(name = "dxfg_order_book_model_listener_function")
public interface DxfgOrderBookModelListenerFunction extends CFunctionPointer {

  @InvokeCFunctionPointer
  void invoke(
      final IsolateThread thread,
      final DxfgOrderBookModel orderBookModel,
      final VoidPointer userData
  );
}