package com.dxfeed.event.market;

import com.dxfeed.api.events.DxfgEventKind;
import com.dxfeed.api.events.DxfgGreeks;
import com.dxfeed.event.option.Greeks;
import org.graalvm.nativeimage.c.struct.SizeOf;

public class GreeksMapper extends Mapper<Greeks, DxfgGreeks> {

  private final MarketEventMapper marketEventMapper;

  public GreeksMapper(final MarketEventMapper marketEventMapper) {
    this.marketEventMapper = marketEventMapper;
  }

  @Override
  protected int size() {
    return SizeOf.get(DxfgGreeks.class);
  }

  @Override
  protected void fillNativeObject(final DxfgGreeks nObject, final Greeks jObject) {
    nObject.setKind(DxfgEventKind.DXFG_EVENT_TYPE_GREEKS.getCValue());
    this.marketEventMapper.fillNativeObject(nObject, jObject);
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
    this.marketEventMapper.cleanNativeObject(nObject);
  }
}
