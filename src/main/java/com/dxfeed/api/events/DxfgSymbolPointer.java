package com.dxfeed.api.events;

import org.graalvm.nativeimage.c.CContext;
import org.graalvm.nativeimage.c.struct.CPointerTo;
import org.graalvm.word.PointerBase;

@CContext(Directives.class)
@CPointerTo(DxfgSymbol.class)
public interface DxfgSymbolPointer extends PointerBase {

  DxfgSymbolPointer addressOf(int index);

  DxfgSymbol read();

  void write(DxfgSymbol data);
}
