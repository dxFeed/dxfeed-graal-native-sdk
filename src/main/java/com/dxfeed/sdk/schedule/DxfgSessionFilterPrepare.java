package com.dxfeed.sdk.schedule;

import com.dxfeed.schedule.SessionFilter;
import java.util.HashMap;
import java.util.Map;
import org.graalvm.nativeimage.c.CContext;
import org.graalvm.nativeimage.c.constant.CEnum;
import org.graalvm.nativeimage.c.constant.CEnumLookup;
import org.graalvm.nativeimage.c.constant.CEnumValue;

@CContext(Directives.class)
@CEnum("dxfg_session_filter_prepare_t")
public enum DxfgSessionFilterPrepare {
  DXFG_SESSION_FILTER_ANY(SessionFilter.ANY),
  DXFG_SESSION_FILTER_TRADING(SessionFilter.TRADING),
  DXFG_SESSION_FILTER_NON_TRADING(SessionFilter.NON_TRADING),
  DXFG_SESSION_FILTER_NO_TRADING(SessionFilter.NO_TRADING),
  DXFG_SESSION_FILTER_PRE_MARKET(SessionFilter.PRE_MARKET),
  DXFG_SESSION_FILTER_REGULAR(SessionFilter.REGULAR),
  DXFG_SESSION_FILTER_AFTER_MARKET(SessionFilter.AFTER_MARKET),
  ;

  private static final Map<SessionFilter, DxfgSessionFilterPrepare> map = new HashMap<>();

  static {
    map.put(SessionFilter.ANY, DXFG_SESSION_FILTER_ANY);
    map.put(SessionFilter.TRADING, DXFG_SESSION_FILTER_TRADING);
    map.put(SessionFilter.NON_TRADING, DXFG_SESSION_FILTER_NON_TRADING);
    map.put(SessionFilter.NO_TRADING, DXFG_SESSION_FILTER_NO_TRADING);
    map.put(SessionFilter.PRE_MARKET, DXFG_SESSION_FILTER_PRE_MARKET);
    map.put(SessionFilter.REGULAR, DXFG_SESSION_FILTER_REGULAR);
    map.put(SessionFilter.AFTER_MARKET, DXFG_SESSION_FILTER_AFTER_MARKET);
  }

  DxfgSessionFilterPrepare(final SessionFilter sessionFilter) {
    this.sessionFilter = sessionFilter;
  }

  public static DxfgSessionFilterPrepare of(final SessionFilter filter) {
    return map.get(filter);
  }

  public final SessionFilter sessionFilter;

  @CEnumLookup
  public static native DxfgSessionFilterPrepare fromCValue(int value);

  @CEnumValue
  public native int getCValue();
}
