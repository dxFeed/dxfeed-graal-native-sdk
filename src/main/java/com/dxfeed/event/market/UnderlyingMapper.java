package com.dxfeed.event.market;

import com.dxfeed.api.events.DxfgEventKind;
import com.dxfeed.api.events.DxfgUnderlying;
import com.dxfeed.event.option.Underlying;
import org.graalvm.nativeimage.c.struct.SizeOf;

public class UnderlyingMapper extends Mapper<Underlying, DxfgUnderlying> {

  private final MarketEventMapper marketEventMapper;

  public UnderlyingMapper(final MarketEventMapper marketEventMapper) {
    this.marketEventMapper = marketEventMapper;
  }

  @Override
  protected int size() {
    return SizeOf.get(DxfgUnderlying.class);
  }

  @Override
  protected void fillNativeObject(final DxfgUnderlying nObject, final Underlying jObject) {
    nObject.setKind(DxfgEventKind.DXFG_EVENT_TYPE_UNDERLYING.getCValue());
    this.marketEventMapper.fillNativeObject(nObject, jObject);
    nObject.setEventFlags(jObject.getEventFlags());
    nObject.setIndex(jObject.getIndex());
    nObject.setVolatility(jObject.getVolatility());
    nObject.setBackVolatility(jObject.getBackVolatility());
    nObject.setCallVolume(jObject.getCallVolume());
    nObject.setPutVolume(jObject.getPutVolume());
    nObject.setPutCallRatio(jObject.getPutCallRatio());
  }

  @Override
  protected void cleanNativeObject(final DxfgUnderlying nObject) {
    this.marketEventMapper.cleanNativeObject(nObject);
  }
}
