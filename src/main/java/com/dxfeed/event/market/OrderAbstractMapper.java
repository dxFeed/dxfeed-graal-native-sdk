package com.dxfeed.event.market;

import com.dxfeed.api.Mapper;
import com.dxfeed.api.events.DxfgOrderBase;
import org.graalvm.nativeimage.c.type.CCharPointer;

public abstract class OrderAbstractMapper<V extends OrderBase, T extends DxfgOrderBase>
    extends MarketEventMapper<V, T> {

  public OrderAbstractMapper(
      final Mapper<String, CCharPointer> stringMapperForMarketEvent
  ) {
    super(stringMapperForMarketEvent);
  }

  @Override
  public void fillNative(final V jObject, final T nObject) {
    super.fillNative(jObject, nObject);
    nObject.setEventFlags(jObject.getEventFlags());
    nObject.setIndex(jObject.getIndex());
    nObject.setTimeSequence(jObject.getTimeSequence());
    nObject.setTimeNanoPart(jObject.getTimeNanoPart());
    nObject.setActionTime(jObject.getActionTime());
    nObject.setOrderId(jObject.getOrderId());
    nObject.setAuxOrderId(jObject.getAuxOrderId());
    nObject.setPrice(jObject.getPrice());
    nObject.setSize(jObject.getSizeAsDouble());
    nObject.setExecutedSize(jObject.getExecutedSize());
    nObject.setCount(jObject.getCount());
    nObject.setFlags(jObject.getFlags());
    nObject.setTradeId(jObject.getTradeId());
    nObject.setTradePrice(jObject.getTradePrice());
    nObject.setTradeSize(jObject.getTradeSize());
  }

  @Override
  public void cleanNative(final T nObject) {
    super.cleanNative(nObject);
  }

  @Override
  public void fillJava(final T nObject, final V jObject) {
    super.fillJava(nObject, jObject);
    jObject.setEventFlags(nObject.getEventFlags());
    jObject.setIndex(nObject.getIndex());
    jObject.setTimeSequence(nObject.getTimeSequence());
    jObject.setTimeNanoPart(nObject.getTimeNanoPart());
    jObject.setActionTime(nObject.getActionTime());
    jObject.setOrderId(nObject.getOrderId());
    jObject.setAuxOrderId(nObject.getAuxOrderId());
    jObject.setPrice(nObject.getPrice());
    jObject.setSizeAsDouble(nObject.getSize());
    jObject.setExecutedSize(nObject.getExecutedSize());
    jObject.setCount(nObject.getCount());
    jObject.setFlags(nObject.getFlags());
    jObject.setTradeId(nObject.getTradeId());
    jObject.setTradePrice(nObject.getTradePrice());
    jObject.setTradeSize(nObject.getTradeSize());
  }
}
