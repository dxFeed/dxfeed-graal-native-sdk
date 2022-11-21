package com.dxfeed.event.market;

import com.dxfeed.api.events.DxfgEventClazz;
import com.dxfeed.api.events.DxfgGreeks;
import com.dxfeed.event.option.Greeks;
import org.graalvm.nativeimage.UnmanagedMemory;
import org.graalvm.nativeimage.c.struct.SizeOf;
import org.graalvm.nativeimage.c.type.CCharPointer;

public class GreeksMapper extends MarketEventMapper<Greeks, DxfgGreeks> {

  public GreeksMapper(
      final Mapper<String, CCharPointer> stringMapperForMarketEvent
  ) {
    super(stringMapperForMarketEvent);
  }

  @Override
  public DxfgGreeks createNativeObject() {
    final DxfgGreeks nObject = UnmanagedMemory.calloc(SizeOf.get(DxfgGreeks.class));
    nObject.setClazz(DxfgEventClazz.DXFG_EVENT_GREEKS.getCValue());
    return nObject;
  }

  @Override
  public void fillNativeObject(final Greeks jObject, final DxfgGreeks nObject) {
    super.fillNativeObject(jObject, nObject);
    nObject.setEventFlags(jObject.getEventFlags());
    nObject.setIndex(jObject.getIndex());
    nObject.setPrice(jObject.getPrice());
    nObject.setVolatility(jObject.getVolatility());
    nObject.setDelta(jObject.getDelta());
    nObject.setGamma(jObject.getGamma());
    nObject.setTheta(jObject.getTheta());
    nObject.setRho(jObject.getRho());
    nObject.setVega(jObject.getVega());
  }

  @Override
  protected void cleanNativeObject(final DxfgGreeks nObject) {
    super.cleanNativeObject(nObject);
  }

  @Override
  public Greeks toJavaObject(final DxfgGreeks nObject) {
    final Greeks jObject = new Greeks();
    fillJavaObject(nObject, jObject);
    return jObject;
  }

  @Override
  public void fillJavaObject(final DxfgGreeks nObject, final Greeks jObject) {
    super.fillJavaObject(nObject, jObject);
    jObject.setEventFlags(nObject.getEventFlags());
    jObject.setIndex(nObject.getIndex());
    jObject.setPrice(nObject.getPrice());
    jObject.setVolatility(nObject.getVolatility());
    jObject.setDelta(nObject.getDelta());
    jObject.setGamma(nObject.getGamma());
    jObject.setTheta(nObject.getTheta());
    jObject.setRho(nObject.getRho());
    jObject.setVega(nObject.getVega());
  }
}
