package com.dxfeed.event.market;

import com.dxfeed.api.events.DxfgEventKind;
import com.dxfeed.api.events.DxfgTheoPrice;
import com.dxfeed.event.option.TheoPrice;
import org.graalvm.nativeimage.c.struct.SizeOf;

public class TheoPriceMapper extends Mapper<TheoPrice, DxfgTheoPrice> {

  private final MarketEventMapper marketEventMapper;

  public TheoPriceMapper(final MarketEventMapper marketEventMapper) {
    this.marketEventMapper = marketEventMapper;
  }

  @Override
  protected int size() {
    return SizeOf.get(DxfgTheoPrice.class);
  }

  @Override
  protected void fillNativeObject(final DxfgTheoPrice nObject, final TheoPrice jObject) {
    nObject.setKind(DxfgEventKind.DXFG_EVENT_TYPE_THEO_PRICE.getCValue());
    this.marketEventMapper.fillNativeObject(nObject, jObject);
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
  protected void cleanNativeObject(final DxfgTheoPrice nObject) {
    this.marketEventMapper.cleanNativeObject(nObject);
  }
}
