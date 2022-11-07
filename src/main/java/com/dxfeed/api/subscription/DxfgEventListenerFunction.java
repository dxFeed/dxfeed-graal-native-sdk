package com.dxfeed.api.subscription;

import com.dxfeed.api.events.DxfgEventPointer;
import org.graalvm.nativeimage.IsolateThread;
import org.graalvm.nativeimage.c.function.CFunctionPointer;
import org.graalvm.nativeimage.c.function.InvokeCFunctionPointer;

public interface DxfgEventListenerFunction extends CFunctionPointer {

  @InvokeCFunctionPointer
  void invoke(
      final IsolateThread thread,
      final DxfgEventPointer events,
      final int size
  );
}
