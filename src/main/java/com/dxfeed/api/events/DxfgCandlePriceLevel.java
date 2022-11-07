package com.dxfeed.api.events;

import org.graalvm.nativeimage.c.CContext;
import org.graalvm.nativeimage.c.struct.CField;
import org.graalvm.nativeimage.c.struct.CStruct;
import org.graalvm.word.PointerBase;

@CContext(Directives.class)
@CStruct("dxfg_candle_price_level_t")
public interface DxfgCandlePriceLevel extends PointerBase {

  @CField("value")
  double getValuePriceLevel();

  @CField("value")
  void setValuePriceLevel(double value);
}
