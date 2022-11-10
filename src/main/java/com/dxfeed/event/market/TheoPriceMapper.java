package com.dxfeed.event.market;

import com.dxfeed.api.events.DxfgEventKind;
import com.dxfeed.api.events.DxfgTheoPrice;
import com.dxfeed.event.option.TheoPrice;
import org.graalvm.nativeimage.c.struct.SizeOf;

public class TheoPriceMapper extends MarketEventMapper<TheoPrice, DxfgTheoPrice> {

  public TheoPriceMapper(final StringMapper stringMapper) {
    super(stringMapper);
  }

  @Override
  protected int size() {
    return SizeOf.get(DxfgTheoPrice.class);
  }

  @Override
  protected void doFillNativeObject(final DxfgTheoPrice nObject, final TheoPrice jObject) {
    nObject.setKind(DxfgEventKind.DXFG_EVENT_TYPE_THEO_PRICE.getCValue());
    nObject.setEventFlags(jObject.getEventFlags());
    nObject.setIndex(jObject.getIndex());
    nObject.setPrice(jObject.getPrice());
    nObject.setUnderlyingPrice(jObject.getUnderlyingPrice());
    nObject.setDelta(jObject.getDelta());
    nObject.setGamma(jObject.getGamma());
    nObject.setDividend(jObject.getDividend());
    nObject.setInterest(jObject.getInterest());
  }

  @Override
  protected void doCleanNativeObject(final DxfgTheoPrice nObject) {
    // nothing
  }
}
