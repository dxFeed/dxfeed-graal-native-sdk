package com.dxfeed.event.market;

import com.dxfeed.sdk.events.DxfgMarketEvent;
import com.dxfeed.sdk.mappers.Mapper;
import org.graalvm.nativeimage.c.type.CCharPointer;

public abstract class MarketEventMapper<T extends MarketEvent, V extends DxfgMarketEvent>
    extends EventMapper<T, V> {

  private final Mapper<String, CCharPointer> stringMapper;

  public MarketEventMapper(final Mapper<String, CCharPointer> stringMapperForMarketEvent) {
    this.stringMapper = stringMapperForMarketEvent;
  }

  @Override
  public void fillNative(final T jObject, final V nObject, boolean clean) {
    if (clean) {
      cleanNative(nObject);
    }

    nObject.setEventSymbol(this.stringMapper.toNative(jObject.getEventSymbol()));
    nObject.setEventTime(jObject.getEventTime());
  }

  @Override
  public void cleanNative(final V nObject) {
    this.stringMapper.release(nObject.getEventSymbol());
  }

  @Override
  public void fillJava(final V nObject, final T jObject) {
    jObject.setEventSymbol(this.stringMapper.toJava(nObject.getEventSymbol()));
    jObject.setEventTime(nObject.getEventTime());
  }

  @Override
  public V createNativeObject(final String symbol) {
    final V nObject = createNativeObject();
    nObject.setEventSymbol(this.stringMapper.toNative(symbol));
    return nObject;
  }
}
