package com.dxfeed.mapper;

import com.dxfeed.NativeLibApi;
import com.dxfeed.event.market.MarketEvent;
import com.dxfeed.event.market.Quote;
import com.dxfeed.event.market.TimeAndSale;
import org.graalvm.nativeimage.UnmanagedMemory;
import org.graalvm.nativeimage.c.struct.SizeOf;

import java.util.List;

public class ListEventMapper implements ListNativeMapper<MarketEvent, NativeLibApi.EventNativePtr> {
    private static final QuotaNativeMapper QUOTA_NATIVE_MAPPER = new QuotaNativeMapper();
    private static final TimeAndSaleNativeMapper TIME_AND_SALE_NATIVE_MAPPER = new TimeAndSaleNativeMapper();

    @Override
    public NativeLibApi.EventNativePtr nativeObject(final List<MarketEvent> jObjects) {
        final NativeLibApi.EventNativePtr nativeEvents = UnmanagedMemory.calloc(SizeOf.get(NativeLibApi.EventNativePtr.class) * jObjects.size());
        for (int i = 0; i < jObjects.size(); i++) {
            final MarketEvent marketEvent = jObjects.get(i);
            final NativeLibApi.EventNative nativeObject;
            if (marketEvent instanceof Quote) {
                nativeObject = QUOTA_NATIVE_MAPPER.nativeObject((Quote) marketEvent);
            } else {
                nativeObject = TIME_AND_SALE_NATIVE_MAPPER.nativeObject((TimeAndSale) marketEvent);
            }
            nativeEvents.addressOf(i).write(nativeObject);
        }
        return nativeEvents;
    }

    @Override
    public void delete(final NativeLibApi.EventNativePtr cObject, final int size) {
        for (int i = 0; i < size; i++) {
            final NativeLibApi.EventNative eventNative = cObject.addressOf(i).read();
            switch (NativeLibApi.EventsTypes.fromCValue(eventNative.getEventType())) {
                case DXF_EVENT_TYPE_QUOTE:
                    QUOTA_NATIVE_MAPPER.delete((NativeLibApi.QuoteNative) eventNative);
                    break;
                case DXF_EVENT_TYPE_TIME_AND_SALE:
                    TIME_AND_SALE_NATIVE_MAPPER.delete((NativeLibApi.TimeAndSaleNative) eventNative);
                    break;
            }
        }
        UnmanagedMemory.free(cObject);
    }
}
