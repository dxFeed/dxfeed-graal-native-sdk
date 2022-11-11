package com.dxfeed.event.market;

import com.dxfeed.api.events.DxfgOrderBase;

public abstract class OrderAbstractMapper<V extends OrderBase, T extends DxfgOrderBase> extends
    Mapper<V, T> {

  private final MarketEventMapper marketEventMapper;

  public OrderAbstractMapper(final MarketEventMapper marketEventMapper) {
    this.marketEventMapper = marketEventMapper;
  }

  @Override
  protected void fillNativeObject(final T nObject, final V jObject) {
    this.marketEventMapper.fillNativeObject(nObject, jObject);
    nObject.setEventFlags(jObject.getEventFlags());
    nObject.setIndex(jObject.getIndex());
    nObject.setTimeSequence(jObject.getTimeSequence());
    nObject.setTimeNanoPart(jObject.getTimeNanoPart());
    nObject.setActionTime(jObject.getActionTime());
    nObject.setOrderId(jObject.getOrderId());
    nObject.setAuxOrderId(jObject.getAuxOrderId());
    nObject.setPrice(jObject.getPrice());
    nObject.setSize(jObject.getSize());
    nObject.setExecutedSize(jObject.getExecutedSize());
    nObject.setCount(jObject.getCount());
    nObject.setFlags(jObject.getFlags());
    nObject.setTradeId(jObject.getTradeId());
    nObject.setTradePrice(jObject.getTradePrice());
    nObject.setTradeSize(jObject.getTradeSize());
  }

  @Override
  protected void cleanNativeObject(final T nObject) {
    this.marketEventMapper.cleanNativeObject(nObject);
  }
}
