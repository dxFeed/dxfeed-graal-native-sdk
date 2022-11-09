package com.dxfeed.event.market;

import com.dxfeed.api.events.DxfgCandleExchange;
import com.dxfeed.event.candle.CandleExchange;
import org.graalvm.nativeimage.c.struct.SizeOf;

public class CandleExchangeMapper extends Mapper<CandleExchange, DxfgCandleExchange> {

  @Override
  protected int size() {
    return SizeOf.get(DxfgCandleExchange.class);
  }

  @Override
  protected void fillNativeObject(final DxfgCandleExchange nObject, final CandleExchange jObject) {
    nObject.setExchangeCode(jObject.getExchangeCode());
  }

  @Override
  protected void cleanNativeObject(final DxfgCandleExchange nObject) {
    // nothing
  }
}
