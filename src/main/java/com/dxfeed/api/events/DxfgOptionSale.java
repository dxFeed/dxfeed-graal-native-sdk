package com.dxfeed.api.events;

import org.graalvm.nativeimage.c.CContext;
import org.graalvm.nativeimage.c.struct.CField;
import org.graalvm.nativeimage.c.struct.CStruct;
import org.graalvm.nativeimage.c.type.CCharPointer;

@CContext(Directives.class)
@CStruct("dxfg_option_sale_t")
public interface DxfgOptionSale extends DxfgMarketEvent {

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

  @CField("time_nano_part")
  int getTimeNanoPart();

  @CField("time_nano_part")
  void setTimeNanoPart(int value);

  @CField("exchange_code")
  char getExchangeCode();

  @CField("exchange_code")
  void setExchangeCode(char value);

  @CField("price")
  double getPrice();

  @CField("price")
  void setPrice(double value);

  @CField("size")
  double getSize();

  @CField("size")
  void setSize(double value);

  @CField("bid_price")
  double getBidPrice();

  @CField("bid_price")
  void setBidPrice(double value);

  @CField("ask_price")
  double getAskPrice();

  @CField("ask_price")
  void setAskPrice(double value);

  @CField("exchange_sale_conditions")
  CCharPointer getExchangeSaleConditions();

  @CField("exchange_sale_conditions")
  void setExchangeSaleConditions(CCharPointer value);

  @CField("flags")
  int getFlags();

  @CField("flags")
  void setFlags(int value);

  @CField("underlying_price")
  double getUnderlyingPrice();

  @CField("underlying_price")
  void setUnderlyingPrice(double value);

  @CField("volatility")
  double getVolatility();

  @CField("volatility")
  void setVolatility(double value);

  @CField("delta")
  double getDelta();

  @CField("delta")
  void setDelta(double value);

  @CField("option_symbol")
  CCharPointer getOptionSymbol();

  @CField("option_symbol")
  void setOptionSymbol(CCharPointer value);
}
