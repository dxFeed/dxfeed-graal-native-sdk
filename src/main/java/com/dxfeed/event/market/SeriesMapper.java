package com.dxfeed.event.market;

import com.dxfeed.event.option.Series;
import com.dxfeed.sdk.events.DxfgEventClazz;
import com.dxfeed.sdk.events.DxfgSeries;
import com.dxfeed.sdk.maper.Mapper;
import org.graalvm.nativeimage.UnmanagedMemory;
import org.graalvm.nativeimage.c.struct.SizeOf;
import org.graalvm.nativeimage.c.type.CCharPointer;

public class SeriesMapper extends MarketEventMapper<Series, DxfgSeries> {

  public SeriesMapper(
      final Mapper<String, CCharPointer> stringMapperForMarketEvent
  ) {
    super(stringMapperForMarketEvent);
  }

  @Override
  public DxfgSeries createNativeObject() {
    final DxfgSeries nObject = UnmanagedMemory.calloc(SizeOf.get(DxfgSeries.class));
    nObject.setClazz(DxfgEventClazz.DXFG_EVENT_SERIES.getCValue());
    return nObject;
  }

  @Override
  public void fillNative(final Series jObject, final DxfgSeries nObject) {
    super.fillNative(jObject, nObject);
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
  public void cleanNative(final DxfgSeries nObject) {
    super.cleanNative(nObject);
  }

  @Override
  protected Series doToJava(final DxfgSeries nObject) {
    final Series jObject = new Series();
    this.fillJava(nObject, jObject);
    return jObject;
  }

  @Override
  public void fillJava(final DxfgSeries nObject, final Series jObject) {
    super.fillJava(nObject, jObject);
    jObject.setEventFlags(nObject.getEventFlags());
    jObject.setIndex(nObject.getIndex());
    jObject.setTimeSequence(nObject.getTimeSequence());
    jObject.setExpiration(nObject.getExpiration());
    jObject.setVolatility(nObject.getVolatility());
    jObject.setCallVolume(nObject.getCallVolume());
    jObject.setPutVolume(nObject.getPutVolume());
    jObject.setPutCallRatio(nObject.getPutCallRatio());
    jObject.setForwardPrice(nObject.getForwardPrice());
    jObject.setDividend(nObject.getDividend());
    jObject.setInterest(nObject.getInterest());
  }
}
