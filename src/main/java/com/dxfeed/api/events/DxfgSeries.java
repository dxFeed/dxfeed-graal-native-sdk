package com.dxfeed.api.events;

import org.graalvm.nativeimage.c.CContext;
import org.graalvm.nativeimage.c.struct.CField;
import org.graalvm.nativeimage.c.struct.CStruct;

@CContext(Directives.class)
@CStruct("dxfg_series_t")
public interface DxfgSeries extends DxfgMarketEvent {

  @CField("event_flags")
  int getEventFlags();

  @CField("event_flags")
  void setEventFlags(int value);

  @CField("index")
  long getIndex();

  @CField("index")
  void setIndex(long value);

  @CField("time_sequence")
  long getTimeSequence();

  @CField("time_sequence")
  void setTimeSequence(long value);

  @CField("expiration")
  int getExpiration();

  @CField("expiration")
  void setExpiration(int value);

  @CField("volatility")
  double getVolatility();

  @CField("volatility")
  void setVolatility(double value);

  @CField("call_volume")
  double getCallVolume();

  @CField("call_volume")
  void setCallVolume(double value);

  @CField("put_volume")
  double getPutVolume();

  @CField("put_volume")
  void setPutVolume(double value);

  @CField("put_call_ratio")
  double getPutCallRatio();

  @CField("put_call_ratio")
  void setPutCallRatio(double value);

  @CField("forward_price")
  double getForwardPrice();

  @CField("forward_price")
  void setForwardPrice(double value);

  @CField("dividend")
  double getDividend();

  @CField("dividend")
  void setDividend(double value);

  @CField("interest")
  double getInterest();

  @CField("interest")
  void setInterest(double value);
}
