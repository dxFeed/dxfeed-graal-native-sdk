package com.dxfeed.api.events;

import org.graalvm.nativeimage.c.CContext;
import org.graalvm.nativeimage.c.struct.CField;
import org.graalvm.nativeimage.c.struct.CStruct;
import org.graalvm.nativeimage.c.type.CCharPointer;
import org.graalvm.word.PointerBase;

@CContext(Directives.class)
@CStruct("dxfg_symbol_t")
public interface DxfgSymbol extends PointerBase {

  @CField("symbol_type")
  int getSymbolType();

  @CField("symbol_type")
  void setSymbolType(int value);

  @CField("symbol")
  CCharPointer getSymbolString();

  @CField("symbol")
  void setSymbolString(CCharPointer value);
}
