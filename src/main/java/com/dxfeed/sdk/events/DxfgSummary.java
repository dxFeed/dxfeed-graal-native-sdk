package com.dxfeed.sdk.events;

import org.graalvm.nativeimage.c.CContext;
import org.graalvm.nativeimage.c.struct.CField;
import org.graalvm.nativeimage.c.struct.CStruct;

@CContext(Directives.class)
@CStruct("dxfg_summary_t")
public interface DxfgSummary extends DxfgMarketEvent {

  @CField("day_id")
  int getDayId();

  @CField("day_id")
  void setDayId(int value);

  @CField("day_open_price")
  double getDayOpenPrice();

  @CField("day_open_price")
  void setDayOpenPrice(double value);

  @CField("day_high_price")
  double getDayHighPrice();

  @CField("day_high_price")
  void setDayHighPrice(double value);

  @CField("day_low_price")
  double getDayLowPrice();

  @CField("day_low_price")
  void setDayLowPrice(double value);

  @CField("day_close_price")
  double getDayClosePrice();

  @CField("day_close_price")
  void setDayClosePrice(double value);

  @CField("prev_day_id")
  int getPrevDayId();

  @CField("prev_day_id")
  void setPrevDayId(int value);

  @CField("prev_day_close_price")
  double getPrevDayClosePrice();

  @CField("prev_day_close_price")
  void setPrevDayClosePrice(double value);

  @CField("prev_day_volume")
  double getPrevDayVolume();

  @CField("prev_day_volume")
  void setPrevDayVolume(double value);

  @CField("open_interest")
  long getOpenInterest();

  @CField("open_interest")
  void setOpenInterest(long value);

  @CField("flags")
  int getFlags();

  @CField("flags")
  void setFlags(int value);

}
