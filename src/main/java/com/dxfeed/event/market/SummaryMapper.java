package com.dxfeed.event.market;

import com.dxfeed.sdk.events.DxfgEventClazz;
import com.dxfeed.sdk.events.DxfgSummary;
import com.dxfeed.sdk.mappers.Mapper;
import org.graalvm.nativeimage.UnmanagedMemory;
import org.graalvm.nativeimage.c.struct.SizeOf;
import org.graalvm.nativeimage.c.type.CCharPointer;

public class SummaryMapper extends MarketEventMapper<Summary, DxfgSummary> {

  public SummaryMapper(
      final Mapper<String, CCharPointer> stringMapperForMarketEvent
  ) {
    super(stringMapperForMarketEvent);
  }

  @Override
  public DxfgSummary createNativeObject() {
    final DxfgSummary nObject = UnmanagedMemory.calloc(SizeOf.get(DxfgSummary.class));
    nObject.setClazz(DxfgEventClazz.DXFG_EVENT_SUMMARY.getCValue());
    return nObject;
  }

  @Override
  public void fillNative(final Summary jObject, final DxfgSummary nObject, boolean clean) {
    super.fillNative(jObject, nObject, clean);
    nObject.setDayId(jObject.getDayId());
    nObject.setDayOpenPrice(jObject.getDayOpenPrice());
    nObject.setDayHighPrice(jObject.getDayHighPrice());
    nObject.setDayLowPrice(jObject.getDayLowPrice());
    nObject.setDayClosePrice(jObject.getDayClosePrice());
    nObject.setPrevDayId(jObject.getPrevDayId());
    nObject.setPrevDayClosePrice(jObject.getPrevDayClosePrice());
    nObject.setPrevDayVolume(jObject.getPrevDayVolume());
    nObject.setOpenInterest(jObject.getOpenInterest());
    nObject.setFlags(jObject.getFlags());
  }

  @Override
  public void cleanNative(final DxfgSummary nObject) {
    super.cleanNative(nObject);
  }

  @Override
  protected Summary doToJava(final DxfgSummary nObject) {
    final Summary jObject = new Summary();
    this.fillJava(nObject, jObject);
    return jObject;
  }

  @Override
  public void fillJava(final DxfgSummary nObject, final Summary jObject) {
    super.fillJava(nObject, jObject);
    jObject.setDayId(nObject.getDayId());
    jObject.setDayOpenPrice(nObject.getDayOpenPrice());
    jObject.setDayHighPrice(nObject.getDayHighPrice());
    jObject.setDayLowPrice(nObject.getDayLowPrice());
    jObject.setDayClosePrice(nObject.getDayClosePrice());
    jObject.setPrevDayId(nObject.getPrevDayId());
    jObject.setPrevDayClosePrice(nObject.getPrevDayClosePrice());
    jObject.setPrevDayVolume(nObject.getPrevDayVolume());
    jObject.setOpenInterest(nObject.getOpenInterest());
    jObject.setFlags(nObject.getFlags());
  }
}
