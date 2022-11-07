package com.dxfeed.api.events;

import org.graalvm.nativeimage.c.CContext;
import org.graalvm.nativeimage.c.struct.CField;
import org.graalvm.nativeimage.c.struct.CStruct;
import org.graalvm.word.PointerBase;

@CContext(Directives.class)
@CStruct("dxfg_candle_exchange_t")
public interface DxfgCandleExchange extends PointerBase {

  @CField("exchange_code")
  char getExchangeCode();

  @CField("exchange_code")
  void setExchangeCode(char exchangeCode);
}
