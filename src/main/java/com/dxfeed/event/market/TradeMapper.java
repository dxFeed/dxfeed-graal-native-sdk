package com.dxfeed.event.market;

import com.dxfeed.api.events.DxfgEventKind;
import com.dxfeed.api.events.DxfgTrade;
import org.graalvm.nativeimage.c.struct.SizeOf;

public class TradeMapper extends TradeBaseMapper<Trade, DxfgTrade> {

  public TradeMapper(final MarketEventMapper marketEventMapper) {
    super(marketEventMapper);
  }

  @Override
  protected int size() {
    return SizeOf.get(DxfgTrade.class);
  }

  @Override
  protected void fillNativeObject(final DxfgTrade nObject, final Trade jObject) {
    nObject.setKind(DxfgEventKind.DXFG_EVENT_TYPE_TRADE.getCValue());
    super.fillNativeObject(nObject, jObject);
  }

  @Override
  protected void cleanNativeObject(final DxfgTrade nObject) {
    super.cleanNativeObject(nObject);
  }
}
