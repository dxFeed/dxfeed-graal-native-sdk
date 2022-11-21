package com.dxfeed.event.market;

import com.dxfeed.api.events.DxfgMarketEvent;
import org.graalvm.nativeimage.c.type.CCharPointer;

public abstract class MarketEventMapper<T extends MarketEvent, V extends DxfgMarketEvent>
    extends EventMapper<T, V> {

  private final Mapper<String, CCharPointer> stringMapper;

  public MarketEventMapper(final Mapper<String, CCharPointer> stringMapperForMarketEvent) {
    this.stringMapper = stringMapperForMarketEvent;
  }

  @Override
  public void fillNativeObject(final T jObject, final V nObject) {
//    cleanNativeObject(nObject);
    nObject.setEventSymbol(this.stringMapper.toNativeObject(jObject.getEventSymbol()));
    nObject.setEventTime(jObject.getEventTime());
  }

  @Override
  protected void cleanNativeObject(final V nObject) {
    this.stringMapper.release(nObject.getEventSymbol());
  }

  @Override
  public void fillJavaObject(final V nObject, final T jObject) {
    jObject.setEventSymbol(this.stringMapper.toJavaObject(nObject.getEventSymbol()));
    jObject.setEventTime(nObject.getEventTime());
  }

  @Override
  public V createNativeObject(final String symbol) {
    final V nObject = createNativeObject();
    nObject.setEventSymbol(this.stringMapper.toNativeObject(symbol));
    return nObject;
  }
}
