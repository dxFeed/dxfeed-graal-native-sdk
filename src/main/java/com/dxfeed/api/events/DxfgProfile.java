package com.dxfeed.api.events;

import org.graalvm.nativeimage.c.CContext;
import org.graalvm.nativeimage.c.struct.CField;
import org.graalvm.nativeimage.c.struct.CStruct;
import org.graalvm.nativeimage.c.type.CCharPointer;

@CContext(Directives.class)
@CStruct("dxfg_profile_t")
public interface DxfgProfile extends DxfgMarketEvent {

  @CField("description")
  CCharPointer getDescription();

  @CField("description")
  void setDescription(CCharPointer value);

  @CField("status_reason")
  CCharPointer getStatusReason();

  @CField("status_reason")
  void setStatusReason(CCharPointer value);

  @CField("halt_start_time")
  long getHaltStartTime();

  @CField("halt_start_time")
  void setHaltStartTime(long value);

  @CField("halt_end_time")
  long getHaltEndTime();

  @CField("halt_end_time")
  void setHaltEndTime(long value);

  @CField("high_limit_price")
  double getHighLimitPrice();

  @CField("high_limit_price")
  void setHighLimitPrice(double value);

  @CField("low_limit_price")
  double getLowLimitPrice();

  @CField("low_limit_price")
  void setLowLimitPrice(double value);

  @CField("high_52_week_price")
  double getHigh52WeekPrice();

  @CField("high_52_week_price")
  void setHigh52WeekPrice(double value);

  @CField("low_52_week_price")
  double getLow52WeekPrice();

  @CField("low_52_week_price")
  void setLow52WeekPrice(double value);

  @CField("beta")
  double getBeta();

  @CField("beta")
  void setBeta(double value);

  @CField("earnings_per_share")
  double getEarningsPerShare();

  @CField("earnings_per_share")
  void setEarningsPerShare(double value);

  @CField("dividend_frequency")
  double getDividendFrequency();

  @CField("dividend_frequency")
  void setDividendFrequency(double value);

  @CField("ex_dividend_amount")
  double getExDividendAmount();

  @CField("ex_dividend_amount")
  void setExDividendAmount(double value);

  @CField("ex_dividend_day_id")
  int getExDividendDayId();

  @CField("ex_dividend_day_id")
  void setExDividendDayId(int value);

  @CField("shares")
  double getShares();

  @CField("shares")
  void setShares(double value);

  @CField("free_float")
  double getFreeFloat();

  @CField("free_float")
  void setFreeFloat(double value);

  @CField("flags")
  int getFlags();

  @CField("flags")
  void setFlags(int value);
}
