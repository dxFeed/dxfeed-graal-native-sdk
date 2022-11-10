package com.dxfeed.api.events;

import org.graalvm.nativeimage.c.CContext;
import org.graalvm.nativeimage.c.struct.CField;
import org.graalvm.nativeimage.c.struct.CStruct;

@CContext(Directives.class)
@CStruct("dxfg_greeks_t")
public interface DxfgGreeks extends DxfgMarketEvent {

  @CField("event_flags")
  int getEventFlags();

  @CField("event_flags")
  void setEventFlags(int value);

  @CField("index")
  long getIndex();

  @CField("index")
  void setIndex(long value);

  @CField("price")
  double getPrice();

  @CField("price")
  void setPrice(double value);

  @CField("volatility")
  double getVolatility();

  @CField("volatility")
  void setVolatility(double value);

  @CField("delta")
  double getDelta();

  @CField("delta")
  void setDelta(double value);

  @CField("gamma")
  double getGamma();

  @CField("gamma")
  void setGamma(double value);

  @CField("theta")
  double getTheta();

  @CField("theta")
  void setTheta(double value);

  @CField("rho")
  double getRho();

  @CField("rho")
  void setRho(double value);

  @CField("vega")
  double getVega();

  @CField("vega")
  void setVega(double value);
}
