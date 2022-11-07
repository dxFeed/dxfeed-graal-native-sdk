package com.dxfeed.api.events;

import org.graalvm.nativeimage.c.CContext;
import org.graalvm.nativeimage.c.constant.CEnum;
import org.graalvm.nativeimage.c.constant.CEnumLookup;
import org.graalvm.nativeimage.c.constant.CEnumValue;

@CContext(Directives.class)
@CEnum("dxfg_candle_price_t")
public enum DxfgCandlePrice {
  LAST,
  BID,
  ASK,
  MARK,
  SETTLEMENT,
  ;

  @CEnumLookup
  public static native DxfgCandlePrice fromCValue(int value);

  @CEnumValue
  public native int getCValue();
}
