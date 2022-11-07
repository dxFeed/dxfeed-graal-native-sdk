package com.dxfeed.api.events;

import org.graalvm.nativeimage.c.CContext;
import org.graalvm.nativeimage.c.struct.CField;
import org.graalvm.nativeimage.c.struct.CFieldAddress;
import org.graalvm.nativeimage.c.struct.CStruct;

@CContext(Directives.class)
@CStruct("dxfg_candle_t")
public interface DxfgCandle extends DxfgEventType, DxfgCandleSymbol {

  @CFieldAddress("event_symbol")
  DxfgCandleSymbol dxfgCandleSymbol();

  @CField("event_flags")
  int getEventFlags();

  @CField("event_flags")
  void setEventFlags(int eventFlags);

  @CField("event_time")
  long getEventTime();

  @CField("event_time")
  void setEventTime(long eventTime);

  @CField("index")
  long getIndex();

  @CField("index")
  void setIndex(long index);

  @CField("count")
  long getCount();

  @CField("count")
  void setCount(long count);

  @CField("open")
  double getOpen();

  @CField("open")
  void setOpen(double open);

  @CField("high")
  double getHigh();

  @CField("high")
  void setHigh(double high);

  @CField("low")
  double getLow();

  @CField("low")
  void setLow(double low);

  @CField("close")
  double getClose();

  @CField("close")
  void setClose(double close);

  @CField("volume")
  double getVolume();

  @CField("volume")
  void setVolume(double volume);

  @CField("vwap")
  double getVwap();

  @CField("vwap")
  void setVwap(double vwap);

  @CField("bid_volume")
  double getBidVolume();

  @CField("bid_volume")
  void setBidVolume(double bid_volume);

  @CField("ask_volume")
  double getAskVolume();

  @CField("ask_volume")
  void setAskVolume(double ask_volume);

  @CField("imp_volatility")
  double getImpVolatility();

  @CField("imp_volatility")
  void setImpVolatility(double imp_volatility);

  @CField("open_interest")
  double getOpenInterest();

  @CField("open_interest")
  void setOpenInterest(double open_interest);
}
