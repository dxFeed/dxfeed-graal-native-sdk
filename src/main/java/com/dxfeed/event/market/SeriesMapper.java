package com.dxfeed.event.market;

import com.dxfeed.api.events.DxfgEventKind;
import com.dxfeed.api.events.DxfgSeries;
import com.dxfeed.event.option.Series;
import org.graalvm.nativeimage.c.struct.SizeOf;

public class SeriesMapper extends MarketEventMapper<Series, DxfgSeries> {

  public SeriesMapper(final StringMapper stringMapper) {
    super(stringMapper);
  }

  @Override
  protected int size() {
    return SizeOf.get(DxfgSeries.class);
  }

  @Override
  protected void doFillNativeObject(final DxfgSeries nObject, final Series jObject) {
    nObject.setKind(DxfgEventKind.DXFG_EVENT_TYPE_SERIES.getCValue());
    nObject.setEventFlags(jObject.getEventFlags());
    nObject.setIndex(jObject.getIndex());
    nObject.setTimeSequence(jObject.getTimeSequence());
    nObject.setExpiration(jObject.getExpiration());
    nObject.setVolatility(jObject.getVolatility());
    nObject.setCallVolume(jObject.getCallVolume());
    nObject.setPutVolume(jObject.getPutVolume());
    nObject.setPutCallRatio(jObject.getPutCallRatio());
    nObject.setForwardPrice(jObject.getForwardPrice());
    nObject.setDividend(jObject.getDividend());
    nObject.setInterest(jObject.getInterest());
  }

  @Override
  protected void doCleanNativeObject(final DxfgSeries nObject) {
    // nothing
  }
}
