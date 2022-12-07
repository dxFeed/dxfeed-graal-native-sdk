package com.dxfeed.api.symbol;

import org.graalvm.nativeimage.c.CContext;
import org.graalvm.nativeimage.c.constant.CEnum;
import org.graalvm.nativeimage.c.constant.CEnumLookup;
import org.graalvm.nativeimage.c.constant.CEnumValue;

@CContext(Directives.class)
@CEnum("dxfg_symbol_type_t")
public enum DxfgSymbolType {
  STRING,
  CANDLE,
  WILDCARD,
  INDEXED_EVENT_SUBSCRIPTION,
  TIME_SERIES_SUBSCRIPTION,
  ;

  @CEnumLookup
  public static native DxfgSymbolType fromCValue(int value);

  @CEnumValue
  public native int getCValue();
}
