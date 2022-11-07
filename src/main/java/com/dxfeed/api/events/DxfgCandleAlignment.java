package com.dxfeed.api.events;

import org.graalvm.nativeimage.c.CContext;
import org.graalvm.nativeimage.c.constant.CEnum;
import org.graalvm.nativeimage.c.constant.CEnumLookup;
import org.graalvm.nativeimage.c.constant.CEnumValue;

@CContext(Directives.class)
@CEnum("dxfg_candle_alignment_t")
public enum DxfgCandleAlignment {
  MIDNIGHT,
  SESSION,
  ;

  @CEnumLookup
  public static native DxfgCandleAlignment fromCValue(int value);

  @CEnumValue
  public native int getCValue();
}
