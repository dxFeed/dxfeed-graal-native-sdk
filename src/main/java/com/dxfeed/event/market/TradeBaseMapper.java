package com.dxfeed.event.market;

import com.dxfeed.api.events.DxfgTradeBase;

public abstract class TradeBaseMapper<V extends TradeBase, T extends DxfgTradeBase> extends
    MarketEventMapper<V, T> {

  public TradeBaseMapper(final StringMapper stringMapper) {
    super(stringMapper);
  }

  @Override
  protected void doFillNativeObject(final T nObject, final V jObject) {
    nObject.setTimeSequence(jObject.getTimeSequence());
    nObject.setTimeNanoPart(jObject.getTimeNanoPart());
    nObject.setExchangeCode(jObject.getExchangeCode());
    nObject.setPrice(jObject.getPrice());
    nObject.setChange(jObject.getChange());
    nObject.setSize(jObject.getSize());
    nObject.setDayId(jObject.getDayId());
    nObject.setDayVolume(jObject.getDayVolume());
    nObject.setDayTurnover(jObject.getDayTurnover());
    nObject.setFlags(jObject.getFlags());
  }
}
