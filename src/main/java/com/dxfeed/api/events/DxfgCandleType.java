package com.dxfeed.api.events;

import org.graalvm.nativeimage.c.CContext;
import org.graalvm.nativeimage.c.constant.CEnum;
import org.graalvm.nativeimage.c.constant.CEnumLookup;
import org.graalvm.nativeimage.c.constant.CEnumValue;

@CContext(Directives.class)
@CEnum("dxfg_candle_type_t")
public enum DxfgCandleType {
  TICK,
  SECOND,
  MINUTE,
  HOUR,
  DAY,
  WEEK,
  MONTH,
  OPTEXP,
  YEAR,
  VOLUME,
  PRICE,
  PRICE_MOMENTUM,
  PRICE_RENKO,
  ;

  @CEnumLookup
  public static native DxfgCandleType fromCValue(int value);

  @CEnumValue
  public native int getCValue();
}
