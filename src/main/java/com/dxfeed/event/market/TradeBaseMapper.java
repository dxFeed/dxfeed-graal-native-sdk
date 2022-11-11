package com.dxfeed.event.market;

import com.dxfeed.api.events.DxfgTradeBase;

public abstract class TradeBaseMapper<V extends TradeBase, T extends DxfgTradeBase> extends
    Mapper<V, T> {

  private final MarketEventMapper marketEventMapper;

  public TradeBaseMapper(final MarketEventMapper marketEventMapper) {
    this.marketEventMapper = marketEventMapper;
  }

  @Override
  protected void fillNativeObject(final T nObject, final V jObject) {
    this.marketEventMapper.fillNativeObject(nObject, jObject);
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

  @Override
  protected void cleanNativeObject(final T nObject) {
    this.marketEventMapper.cleanNativeObject(nObject);
  }
}
