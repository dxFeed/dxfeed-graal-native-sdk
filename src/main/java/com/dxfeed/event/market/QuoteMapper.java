package com.dxfeed.event.market;

import com.dxfeed.api.events.DxfgEventKind;
import com.dxfeed.api.events.DxfgQuote;
import org.graalvm.nativeimage.c.struct.SizeOf;

public class QuoteMapper extends MarketEventMapper<Quote, DxfgQuote> {

  public QuoteMapper(final StringMapper stringMapper) {
    super(stringMapper);
  }

  @Override
  protected int size() {
    return SizeOf.get(DxfgQuote.class);
  }

  @Override
  protected void doFillNativeObject(final DxfgQuote nObject, final Quote jObject) {
    nObject.setKind(DxfgEventKind.DXFG_EVENT_TYPE_QUOTE.getCValue());
    nObject.setTimeMillisSequence(jObject.getTimeMillisSequence());
    nObject.setTimeNanoPart(jObject.getTimeNanoPart());
    nObject.setBidTime(jObject.getBidTime());
    nObject.setBidExchangeCode(jObject.getBidExchangeCode());
    nObject.setBidPrice(jObject.getBidPrice());
    nObject.setBidSize(jObject.getBidSizeAsDouble());
    nObject.setAskTime(jObject.getAskTime());
    nObject.setAskExchangeCode(jObject.getAskExchangeCode());
    nObject.setAskPrice(jObject.getAskPrice());
    nObject.setAskSize(jObject.getAskSizeAsDouble());
  }

  @Override
  protected void doCleanNativeObject(final DxfgQuote nObject) {
    // nothing
  }
}
