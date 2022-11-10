package com.dxfeed.event.market;

import com.dxfeed.api.events.DxfgEventKind;
import com.dxfeed.api.events.DxfgSummary;
import org.graalvm.nativeimage.c.struct.SizeOf;

public class SummaryMapper extends MarketEventMapper<Summary, DxfgSummary> {

  public SummaryMapper(final StringMapper stringMapper) {
    super(stringMapper);
  }

  @Override
  protected int size() {
    return SizeOf.get(DxfgSummary.class);
  }

  @Override
  protected void doFillNativeObject(final DxfgSummary nObject, final Summary jObject) {
    nObject.setKind(DxfgEventKind.DXFG_EVENT_TYPE_SUMMARY.getCValue());
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
  protected void doCleanNativeObject(final DxfgSummary nObject) {
    // nothing
  }
}
