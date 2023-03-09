package com.dxfeed.sdk.symbol;

import org.graalvm.nativeimage.c.CContext;
import org.graalvm.nativeimage.c.struct.CField;
import org.graalvm.nativeimage.c.struct.CStruct;

@CContext(Directives.class)
@CStruct("dxfg_time_series_subscription_symbol_t")
public interface DxfgTimeSeriesSubscriptionSymbol extends DxfgSymbol {

  @CField("symbol")
  DxfgSymbol getSymbol();

  @CField("symbol")
  void setSymbol(DxfgSymbol value);

  @CField("from_time")
  long getFromTime();

  @CField("from_time")
  void setFromTime(long value);
}
