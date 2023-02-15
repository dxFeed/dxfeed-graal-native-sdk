package com.dxfeed.api.schedule;

import static com.dxfeed.schedule.SessionType.AFTER_MARKET;
import static com.dxfeed.schedule.SessionType.NO_TRADING;
import static com.dxfeed.schedule.SessionType.PRE_MARKET;
import static com.dxfeed.schedule.SessionType.REGULAR;

import com.dxfeed.schedule.SessionType;
import java.util.EnumMap;
import java.util.Map;
import org.graalvm.nativeimage.c.CContext;
import org.graalvm.nativeimage.c.constant.CEnum;
import org.graalvm.nativeimage.c.constant.CEnumLookup;
import org.graalvm.nativeimage.c.constant.CEnumValue;

@CContext(Directives.class)
@CEnum("dxfg_session_type_t")
public enum DxfgSessionType {
  DXFG_SESSION_TYPE_NO_TRADING,
  DXFG_SESSION_TYPE_PRE_MARKET,
  DXFG_SESSION_TYPE_REGULAR,
  DXFG_SESSION_TYPE_AFTER_MARKET,
  ;

  private static final Map<SessionType, DxfgSessionType> map = new EnumMap<>(SessionType.class);

  static {
    map.put(NO_TRADING, DXFG_SESSION_TYPE_NO_TRADING);
    map.put(PRE_MARKET, DXFG_SESSION_TYPE_PRE_MARKET);
    map.put(REGULAR, DXFG_SESSION_TYPE_REGULAR);
    map.put(AFTER_MARKET, DXFG_SESSION_TYPE_AFTER_MARKET);
  }

  public static DxfgSessionType of(final SessionType state) {
    return map.get(state);
  }

  @CEnumLookup
  public static native DxfgSessionType fromCValue(int value);

  @CEnumValue
  public native int getCValue();
}
