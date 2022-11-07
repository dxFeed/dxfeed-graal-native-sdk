package com.dxfeed.api.events;

import org.graalvm.nativeimage.c.CContext;
import org.graalvm.nativeimage.c.struct.CPointerTo;
import org.graalvm.word.PointerBase;

@CContext(Directives.class)
@CPointerTo(DxfgEventType.class)
public interface DxfgEventPointer extends PointerBase {

  DxfgEventPointer addressOf(int index);

  DxfgEventType read();

  void write(DxfgEventType data);
}
