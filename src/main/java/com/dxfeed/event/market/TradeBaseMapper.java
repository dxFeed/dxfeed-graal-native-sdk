package com.dxfeed.event.market;

import com.dxfeed.api.events.DxfgTradeBase;
import org.graalvm.nativeimage.c.type.CCharPointer;

public abstract class TradeBaseMapper<V extends TradeBase, T extends DxfgTradeBase>
    extends MarketEventMapper<V, T> {

  public TradeBaseMapper(
      final Mapper<String, CCharPointer> stringMapperForMarketEvent
  ) {
    super(stringMapperForMarketEvent);
  }

  @Override
  public void fillNativeObject(final V jObject, final T nObject) {
    super.fillNativeObject(jObject, nObject);
    nObject.setTimeSequence(jObject.getTimeSequence());
    nObject.setTimeNanoPart(jObject.getTimeNanoPart());
    nObject.setExchangeCode(jObject.getExchangeCode());
    nObject.setPrice(jObject.getPrice());
    nObject.setChange(jObject.getChange());
    nObject.setSize(jObject.getSizeAsDouble());
    nObject.setDayId(jObject.getDayId());
    nObject.setDayVolume(jObject.getDayVolumeAsDouble());
    nObject.setDayTurnover(jObject.getDayTurnover());
    nObject.setFlags(jObject.getFlags());
  }

  @Override
  protected void cleanNativeObject(final T nObject) {
    super.cleanNativeObject(nObject);
  }

  @Override
  public void fillJavaObject(final T nObject, final V jObject) {
    super.fillJavaObject(nObject, jObject);
    jObject.setTimeSequence(nObject.getTimeSequence());
    jObject.setTimeNanoPart(nObject.getTimeNanoPart());
    jObject.setExchangeCode(nObject.getExchangeCode());
    jObject.setPrice(nObject.getPrice());
    jObject.setChange(nObject.getChange());
    jObject.setSizeAsDouble(nObject.getSize());
    jObject.setDayId(nObject.getDayId());
    jObject.setDayVolumeAsDouble(nObject.getDayVolume());
    jObject.setDayTurnover(nObject.getDayTurnover());
    jObject.setFlags(nObject.getFlags());
  }
}
