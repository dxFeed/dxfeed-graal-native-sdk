package com.dxfeed.event.market;

import com.dxfeed.api.events.DxfgEventClazz;
import com.dxfeed.api.events.DxfgSummary;
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
  public void fillNativeObject(final Summary jObject, final DxfgSummary nObject) {
    super.fillNativeObject(jObject, nObject);
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
  protected void cleanNativeObject(final DxfgSummary nObject) {
    super.cleanNativeObject(nObject);
  }

  @Override
  public Summary toJavaObject(final DxfgSummary nObject) {
    final Summary jObject = new Summary();
    fillJavaObject(nObject, jObject);
    return jObject;
  }

  @Override
  public void fillJavaObject(final DxfgSummary nObject, final Summary jObject) {
    super.fillJavaObject(nObject, jObject);
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
