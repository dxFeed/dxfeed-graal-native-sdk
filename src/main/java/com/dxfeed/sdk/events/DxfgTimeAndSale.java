package com.dxfeed.sdk.events;

import org.graalvm.nativeimage.c.CContext;
import org.graalvm.nativeimage.c.struct.CField;
import org.graalvm.nativeimage.c.struct.CStruct;
import org.graalvm.nativeimage.c.type.CCharPointer;

@CContext(Directives.class)
@CStruct("dxfg_time_and_sale_t")
public interface DxfgTimeAndSale extends DxfgMarketEvent {

  @CField("event_flags")
  int getEventFlags();

  @CField("event_flags")
  void setEventFlags(int eventFlag);

  @CField("index")
  long getIndex();

  @CField("index")
  void setIndex(long index);

  @CField("time_nano_part")
  int getTimeNanoPart();

  @CField("time_nano_part")
  void setTimeNanoPart(int timeNanoPart);

  @CField("exchange_code")
  char getExchangeCode();

  @CField("exchange_code")
  void setExchangeCode(char exchangeCode);

  @CField("price")
  double getPrice();

  @CField("price")
  void setPrice(double price);

  @CField("size")
  double getSize();

  @CField("size")
  void setSize(double size);

  @CField("bid_price")
  double getBidPrice();

  @CField("bid_price")
  void setBidPrice(double bidPrice);

  @CField("ask_price")
  double getAskPrice();

  @CField("ask_price")
  void setAskPrice(double askPrice);

  @CField("exchange_sale_conditions")
  CCharPointer getExchangeSaleConditions();

  @CField("exchange_sale_conditions")
  void setExchangeSaleConditions(CCharPointer exchangeSaleConditions);

  @CField("flags")
  int getFlags();

  @CField("flags")
  void setFlags(int flags);

  @CField("buyer")
  CCharPointer getBuyer();

  @CField("buyer")
  void setBuyer(CCharPointer buyer);

  @CField("seller")
  CCharPointer getSeller();

  @CField("seller")
  void setSeller(CCharPointer seller);

}
