package com.dxfeed.api.symbol;

import org.graalvm.nativeimage.c.CContext;
import org.graalvm.nativeimage.c.struct.CField;
import org.graalvm.nativeimage.c.struct.CStruct;
import org.graalvm.nativeimage.c.type.CCharPointer;

@CContext(Directives.class)
@CStruct("dxfg_string_symbol_t")
public interface DxfgStringSymbol extends DxfgSymbol {

  @CField("symbol")
  CCharPointer getSymbol();

  @CField("symbol")
  void setSymbol(CCharPointer value);
}
