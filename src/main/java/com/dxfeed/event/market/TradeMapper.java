package com.dxfeed.event.market;

import com.dxfeed.api.events.DxfgEventKind;
import com.dxfeed.api.events.DxfgTrade;
import org.graalvm.nativeimage.c.struct.SizeOf;

public class TradeMapper extends TradeBaseMapper<Trade, DxfgTrade> {

  public TradeMapper(final StringMapper stringMapper) {
    super(stringMapper);
  }

  @Override
  protected int size() {
    return SizeOf.get(DxfgTrade.class);
  }

  @Override
  protected void doFillNativeObject(final DxfgTrade nObject, final Trade jObject) {
    nObject.setKind(DxfgEventKind.DXFG_EVENT_TYPE_TRADE.getCValue());
    super.doFillNativeObject(nObject, jObject);
  }

  @Override
  protected void doCleanNativeObject(final DxfgTrade nObject) {
    // nothing
  }
}
