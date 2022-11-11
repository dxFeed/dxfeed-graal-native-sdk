package com.dxfeed.event.market;

import com.dxfeed.api.events.DxfgEventKind;
import com.dxfeed.api.events.DxfgSummary;
import org.graalvm.nativeimage.c.struct.SizeOf;

public class SummaryMapper extends Mapper<Summary, DxfgSummary> {

  private final MarketEventMapper marketEventMapper;

  public SummaryMapper(final MarketEventMapper marketEventMapper) {
    this.marketEventMapper = marketEventMapper;
  }

  @Override
  protected int size() {
    return SizeOf.get(DxfgSummary.class);
  }

  @Override
  protected void fillNativeObject(final DxfgSummary nObject, final Summary jObject) {
    nObject.setKind(DxfgEventKind.DXFG_EVENT_TYPE_SUMMARY.getCValue());
    this.marketEventMapper.fillNativeObject(nObject, jObject);
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
    this.marketEventMapper.cleanNativeObject(nObject);
  }
}
