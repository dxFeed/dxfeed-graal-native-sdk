package com.dxfeed.event.market;

import com.dxfeed.api.events.DxfgMarketEvent;

public abstract class MarketEventMapper
    <V extends MarketEvent, T extends DxfgMarketEvent>
    extends Mapper<V, T> {

  protected final StringMapper stringMapper;

  protected MarketEventMapper(final StringMapper stringMapper) {
    this.stringMapper = stringMapper;
  }

  @Override
  protected final void fillNativeObject(final T nObject, final V jObject) {
    nObject.setEventSymbol(stringMapper.nativeObject(jObject.getEventSymbol()));
    nObject.setEventTime(jObject.getEventTime());
    doFillNativeObject(nObject, jObject);
  }

  @Override
  protected final void cleanNativeObject(final T nObject) {
    stringMapper.delete(nObject.getEventSymbol());
    doCleanNativeObject(nObject);
  }

  protected abstract void doFillNativeObject(final T nObject, final V jObject);

  protected abstract void doCleanNativeObject(final T nObject);
}
