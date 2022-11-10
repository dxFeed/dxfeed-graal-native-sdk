package com.dxfeed.event.market;

import com.dxfeed.api.events.DxfgOrderBase;

public abstract class OrderAbstractMapper<V extends OrderBase, T extends DxfgOrderBase> extends
    MarketEventMapper<V, T> {

  public OrderAbstractMapper(final StringMapper stringMapper) {
    super(stringMapper);
  }

  @Override
  protected void doFillNativeObject(final T nObject, final V jObject) {
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
}
