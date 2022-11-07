package com.dxfeed.event.market;

import com.dxfeed.api.events.DxfgEventKind;
import com.dxfeed.api.events.DxfgQuote;
import org.graalvm.nativeimage.UnmanagedMemory;
import org.graalvm.nativeimage.c.struct.SizeOf;

public class QuoteNativeMapper extends MarketEventNativeMapper<Quote, DxfgQuote> {

  @Override
  protected DxfgQuote createNativeEvent(final Quote quote) {
    final DxfgQuote dxfgQuote = UnmanagedMemory.calloc(SizeOf.get(DxfgQuote.class));
    dxfgQuote.setKind(DxfgEventKind.DXFG_EVENT_TYPE_QUOTE.getCValue());
    dxfgQuote.setTimeMillisSequence(quote.getTimeMillisSequence());
    dxfgQuote.setTimeNanoPart(quote.getTimeNanoPart());
    dxfgQuote.setBidTime(quote.getBidTime());
    dxfgQuote.setBidExchangeCode(quote.getBidExchangeCode());
    dxfgQuote.setBidPrice(quote.getBidPrice());
    dxfgQuote.setBidSize(quote.getBidSizeAsDouble());
    dxfgQuote.setAskTime(quote.getAskTime());
    dxfgQuote.setAskExchangeCode(quote.getAskExchangeCode());
    dxfgQuote.setAskPrice(quote.getAskPrice());
    dxfgQuote.setAskSize(quote.getAskSizeAsDouble());
    return dxfgQuote;
  }

  @Override
  protected void doDelete(final DxfgQuote dxfgQuote) {
    UnmanagedMemory.free(dxfgQuote);
  }
}
