package com.dxfeed.api.events;

import org.graalvm.nativeimage.c.CContext;
import org.graalvm.nativeimage.c.struct.CPointerTo;
import org.graalvm.nativeimage.c.type.CCharPointer;
import org.graalvm.word.PointerBase;

@CContext(Directives.class)
@CPointerTo(CCharPointer.class)
public interface DxfgCCharPointerPointer extends PointerBase {

  DxfgCCharPointerPointer addressOf(int index);

  CCharPointer read();

  void write(CCharPointer data);
}
