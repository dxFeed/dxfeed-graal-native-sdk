package com.dxfeed.event.market;

import com.dxfeed.api.events.DxfgEventKind;
import com.dxfeed.api.events.DxfgTradeETH;
import org.graalvm.nativeimage.c.struct.SizeOf;

public class TradeETHMapper extends TradeBaseMapper<TradeETH, DxfgTradeETH> {

  public TradeETHMapper(final MarketEventMapper marketEventMapper) {
    super(marketEventMapper);
  }

  @Override
  protected int size() {
    return SizeOf.get(DxfgTradeETH.class);
  }

  @Override
  protected void fillNativeObject(final DxfgTradeETH nObject, final TradeETH jObject) {
    nObject.setKind(DxfgEventKind.DXFG_EVENT_TYPE_TRADE_ETH.getCValue());
    super.fillNativeObject(nObject, jObject);
  }

  @Override
  protected void cleanNativeObject(final DxfgTradeETH nObject) {
    super.cleanNativeObject(nObject);
  }
}
