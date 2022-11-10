package com.dxfeed.api.events;

import org.graalvm.nativeimage.c.CContext;
import org.graalvm.nativeimage.c.struct.CField;
import org.graalvm.nativeimage.c.struct.CStruct;

@CContext(Directives.class)
@CStruct("dxfg_underlying_t")
public interface DxfgUnderlying extends DxfgMarketEvent {

  @CField("event_flags")
  int getEventFlags();

  @CField("event_flags")
  void setEventFlags(int value);

  @CField("index")
  long getIndex();

  @CField("index")
  void setIndex(long value);

  @CField("volatility")
  double getVolatility();

  @CField("volatility")
  void setVolatility(double value);

  @CField("front_volatility")
  double getFrontVolatility();

  @CField("front_volatility")
  void setFrontVolatility(double value);

  @CField("back_volatility")
  double getBackVolatility();

  @CField("back_volatility")
  void setBackVolatility(double value);

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

}
