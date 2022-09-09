package com.dxfeed.mapper;

import com.dxfeed.NativeLibApi;
import com.dxfeed.event.market.Quote;
import org.graalvm.nativeimage.UnmanagedMemory;
import org.graalvm.nativeimage.c.struct.SizeOf;

public class QuotaNativeMapper extends BaseEventNativeMapper<Quote, NativeLibApi.QuoteNative> {

    @Override
    protected NativeLibApi.QuoteNative createNativeEvent(final Quote quote) {
        final NativeLibApi.QuoteNative quoteNative = UnmanagedMemory.calloc(SizeOf.get(NativeLibApi.QuoteNative.class));
        quoteNative.setEventType(NativeLibApi.EventsTypes.DXF_EVENT_TYPE_QUOTE.getCValue());
        quoteNative.setAskPrice(quote.getAskPrice());
        quoteNative.setBidPrice(quote.getBidPrice());
        return quoteNative;
    }

    @Override
    protected void doDelete(final NativeLibApi.QuoteNative nativeEvent) {
        UnmanagedMemory.free(nativeEvent);
    }
}
