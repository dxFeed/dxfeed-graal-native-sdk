package com.dxfeed.api.events;

import org.graalvm.nativeimage.ObjectHandle;
import org.graalvm.nativeimage.c.CContext;
import org.graalvm.nativeimage.c.struct.CField;
import org.graalvm.nativeimage.c.struct.CFieldAddress;
import org.graalvm.nativeimage.c.struct.CStruct;
import org.graalvm.nativeimage.c.type.CCharPointer;
import org.graalvm.word.PointerBase;

@CContext(Directives.class)
@CStruct("dxfg_candle_symbol_t")
public interface DxfgCandleSymbol extends DxfgCandleExchange, DxfgCandlePeriod, DxfgCandlePriceLevel, PointerBase {

  @CField("symbol")
  CCharPointer getSymbol();

  @CField("symbol")
  void setSymbol(CCharPointer symbol);

  @CField("base_symbol")
  CCharPointer getBaseSymbol();

  @CField("base_symbol")
  void setBaseSymbol(CCharPointer baseSymbol);

  @CFieldAddress("exchange")
  DxfgCandleExchange getExchange();

  @CField("price")
  int getPrice();

  @CField("price")
  void setPrice(int price);

  @CField("session")
  int getSession();

  @CField("session")
  void setSession(int session);

  @CFieldAddress("period")
  DxfgCandlePeriod getPeriod();

  @CField("alignment")
  int getAlignment();

  @CField("alignment")
  void setAlignment(int alignment);

  @CFieldAddress("price_level")
  DxfgCandlePriceLevel getPriceLevel();
}
