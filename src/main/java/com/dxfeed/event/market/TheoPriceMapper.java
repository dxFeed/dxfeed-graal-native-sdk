package com.dxfeed.event.market;

import com.dxfeed.api.events.DxfgEventClazz;
import com.dxfeed.api.events.DxfgTheoPrice;
import com.dxfeed.event.option.TheoPrice;
import org.graalvm.nativeimage.UnmanagedMemory;
import org.graalvm.nativeimage.c.struct.SizeOf;
import org.graalvm.nativeimage.c.type.CCharPointer;

public class TheoPriceMapper extends MarketEventMapper<TheoPrice, DxfgTheoPrice> {

  public TheoPriceMapper(
      final Mapper<String, CCharPointer> stringMapperForMarketEvent
  ) {
    super(stringMapperForMarketEvent);
  }

  @Override
  public DxfgTheoPrice createNativeObject() {
    final DxfgTheoPrice nObject = UnmanagedMemory.calloc(SizeOf.get(DxfgTheoPrice.class));
    nObject.setClazz(DxfgEventClazz.DXFG_EVENT_THEO_PRICE.getCValue());
    return nObject;
  }

  @Override
  public void fillNativeObject(final TheoPrice jObject, final DxfgTheoPrice nObject) {
    super.fillNativeObject(jObject, nObject);
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
    super.cleanNativeObject(nObject);
  }

  @Override
  public TheoPrice toJavaObject(final DxfgTheoPrice nObject) {
    final TheoPrice jObject = new TheoPrice();
    fillJavaObject(nObject, jObject);
    return jObject;
  }

  @Override
  public void fillJavaObject(final DxfgTheoPrice nObject, final TheoPrice jObject) {
    super.fillJavaObject(nObject, jObject);
    jObject.setEventFlags(nObject.getEventFlags());
    jObject.setIndex(nObject.getIndex());
    jObject.setPrice(nObject.getPrice());
    jObject.setUnderlyingPrice(nObject.getUnderlyingPrice());
    jObject.setDelta(nObject.getDelta());
    jObject.setGamma(nObject.getGamma());
    jObject.setDividend(nObject.getDividend());
    jObject.setInterest(nObject.getInterest());
  }
}
