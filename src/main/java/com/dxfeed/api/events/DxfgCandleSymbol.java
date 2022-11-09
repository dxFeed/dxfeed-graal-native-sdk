package com.dxfeed.api.events;

import org.graalvm.nativeimage.c.CContext;
import org.graalvm.nativeimage.c.struct.CField;
import org.graalvm.nativeimage.c.struct.CStruct;
import org.graalvm.nativeimage.c.type.CCharPointer;
import org.graalvm.word.PointerBase;

@CContext(Directives.class)
@CStruct("dxfg_candle_symbol_t")
public interface DxfgCandleSymbol extends PointerBase {

  @CField("symbol")
  CCharPointer getSymbol();

  @CField("symbol")
  void setSymbol(CCharPointer symbol);

  @CField("base_symbol")
  CCharPointer getBaseSymbol();

  @CField("base_symbol")
  void setBaseSymbol(CCharPointer baseSymbol);

  @CField("exchange")
  DxfgCandleExchange getExchange();

  @CField("exchange")
  void setExchange(DxfgCandleExchange baseSymbol);

  @CField("price")
  int getPrice();

  @CField("price")
  void setPrice(int price);

  @CField("session")
  int getSession();

  @CField("session")
  void setSession(int session);

  @CField("period")
  DxfgCandlePeriod getPeriod();

  @CField("period")
  void setPeriod(DxfgCandlePeriod period);

  @CField("alignment")
  int getAlignment();

  @CField("alignment")
  void setAlignment(int alignment);

  @CField("price_level")
  DxfgCandlePriceLevel getPriceLevel();

  @CField("price_level")
  void setPriceLevel(DxfgCandlePriceLevel priceLevel);
}
