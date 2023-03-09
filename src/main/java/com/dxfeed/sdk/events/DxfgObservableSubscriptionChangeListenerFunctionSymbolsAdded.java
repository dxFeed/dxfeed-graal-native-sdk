package com.dxfeed.sdk.events;

import com.dxfeed.sdk.symbol.DxfgSymbolList;
import com.oracle.svm.core.c.CTypedef;
import org.graalvm.nativeimage.IsolateThread;
import org.graalvm.nativeimage.c.CContext;
import org.graalvm.nativeimage.c.function.CFunctionPointer;
import org.graalvm.nativeimage.c.function.InvokeCFunctionPointer;
import org.graalvm.nativeimage.c.type.VoidPointer;

@CContext(Directives.class)
@CTypedef(name = "dxfg_ObservableSubscriptionChangeListener_function_symbolsAdded")
public interface DxfgObservableSubscriptionChangeListenerFunctionSymbolsAdded
    extends CFunctionPointer {

  @InvokeCFunctionPointer
  void invoke(
      final IsolateThread thread,
      final DxfgSymbolList dxfgSymbolList,
      final VoidPointer userData
  );
}
