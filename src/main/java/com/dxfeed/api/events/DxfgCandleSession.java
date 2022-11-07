package com.dxfeed.api.events;

import org.graalvm.nativeimage.c.CContext;
import org.graalvm.nativeimage.c.constant.CEnum;
import org.graalvm.nativeimage.c.constant.CEnumLookup;
import org.graalvm.nativeimage.c.constant.CEnumValue;

@CContext(Directives.class)
@CEnum("dxfg_candle_session_t")
public enum DxfgCandleSession {
  ANY,
  REGULAR,
  ;

  @CEnumLookup
  public static native DxfgCandleSession fromCValue(int value);

  @CEnumValue
  public native int getCValue();
}
