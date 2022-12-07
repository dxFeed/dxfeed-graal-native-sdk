package com.dxfeed.api.symbol;

import org.graalvm.nativeimage.c.CContext;
import org.graalvm.nativeimage.c.struct.CField;
import org.graalvm.nativeimage.c.struct.CStruct;
import org.graalvm.word.PointerBase;

@CContext(Directives.class)
@CStruct("dxfg_symbol_t")
public interface DxfgSymbol extends PointerBase {

  @CField("type")
  int getType();

  @CField("type")
  void setType(int value);
}
