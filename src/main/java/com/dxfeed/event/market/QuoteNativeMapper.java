// SPDX-License-Identifier: MPL-2.0

package com.dxfeed.event.market;

import com.dxfeed.api.events.EventTypesNative;
import com.dxfeed.api.events.QuoteNative;
import org.graalvm.nativeimage.UnmanagedMemory;
import org.graalvm.nativeimage.c.struct.SizeOf;

public class QuoteNativeMapper extends BaseEventNativeMapper<Quote, QuoteNative> {
    @Override
    protected QuoteNative createNativeEvent(Quote quote) {
        QuoteNative quoteNative = UnmanagedMemory.calloc(SizeOf.get(QuoteNative.class));
        quoteNative.setEventType(EventTypesNative.DXFG_EVENT_TYPE_QUOTE.getCValue());
        quoteNative.setEventTime(quote.getEventTime());
        quoteNative.setTimeMillisSequence(quote.getTimeMillisSequence());
        quoteNative.setTimeNanoPart(quote.getTimeNanoPart());
        quoteNative.setBidTime(quote.getBidTime());
        quoteNative.setBidExchangeCode(quote.getBidExchangeCode());
        quoteNative.setBidPrice(quote.getBidPrice());
        quoteNative.setBidSize(quote.getBidSizeAsDouble());
        quoteNative.setAskTime(quote.getAskTime());
        quoteNative.setAskExchangeCode(quote.getAskExchangeCode());
        quoteNative.setAskPrice(quote.getAskPrice());
        quoteNative.setAskSize(quote.getAskSizeAsDouble());
        return quoteNative;
    }

    @Override
    protected void doDelete(QuoteNative quoteNative) {
        UnmanagedMemory.free(quoteNative);
    }
}
