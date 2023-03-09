package com.dxfeed.sdk.events;

import org.graalvm.nativeimage.c.CContext;
import org.graalvm.nativeimage.c.struct.CField;
import org.graalvm.nativeimage.c.struct.CStruct;
import org.graalvm.nativeimage.c.type.CCharPointer;

@CContext(Directives.class)
@CStruct("dxfg_market_event_t")
public interface DxfgMarketEvent extends DxfgEventType {

  @CField("event_symbol")
  CCharPointer getEventSymbol();

  @CField("event_symbol")
  void setEventSymbol(CCharPointer eventSymbol);

  @CField("event_time")
  long getEventTime();

  @CField("event_time")
  void setEventTime(long eventTime);
}
