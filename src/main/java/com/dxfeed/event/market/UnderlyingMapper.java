package com.dxfeed.event.market;

import com.dxfeed.api.events.DxfgEventKind;
import com.dxfeed.api.events.DxfgUnderlying;
import com.dxfeed.event.option.Underlying;
import org.graalvm.nativeimage.c.struct.SizeOf;

public class UnderlyingMapper extends MarketEventMapper<Underlying, DxfgUnderlying> {

  public UnderlyingMapper(final StringMapper stringMapper) {
    super(stringMapper);
  }

  @Override
  protected int size() {
    return SizeOf.get(DxfgUnderlying.class);
  }

  @Override
  protected void doFillNativeObject(final DxfgUnderlying nObject, final Underlying jObject) {
    nObject.setKind(DxfgEventKind.DXFG_EVENT_TYPE_UNDERLYING.getCValue());
    nObject.setEventFlags(jObject.getEventFlags());
    nObject.setIndex(jObject.getIndex());
    nObject.setVolatility(jObject.getVolatility());
    nObject.setBackVolatility(jObject.getBackVolatility());
    nObject.setCallVolume(jObject.getCallVolume());
    nObject.setPutVolume(jObject.getPutVolume());
    nObject.setPutCallRatio(jObject.getPutCallRatio());
  }

  @Override
  protected void doCleanNativeObject(final DxfgUnderlying nObject) {
    // nothing
  }
}
