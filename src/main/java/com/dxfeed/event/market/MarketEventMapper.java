package com.dxfeed.event.market;

import com.dxfeed.api.events.DxfgMarketEvent;
import org.graalvm.nativeimage.c.struct.SizeOf;
import org.graalvm.nativeimage.c.type.CCharPointer;

public class MarketEventMapper extends Mapper<MarketEvent, DxfgMarketEvent> {

  private final Mapper<String, CCharPointer> stringMapper;

  public MarketEventMapper(final Mapper<String, CCharPointer> stringMapperForMarketEvent) {
    this.stringMapper = stringMapperForMarketEvent;
  }

  @Override
  protected int size() {
    return SizeOf.get(DxfgMarketEvent.class);
  }

  @Override
  protected final void fillNativeObject(final DxfgMarketEvent nObject, final MarketEvent jObject) {
    nObject.setEventSymbol(stringMapper.nativeObject(jObject.getEventSymbol()));
    nObject.setEventTime(jObject.getEventTime());
  }

  @Override
  protected final void cleanNativeObject(final DxfgMarketEvent nObject) {
    stringMapper.delete(nObject.getEventSymbol());
  }
}
