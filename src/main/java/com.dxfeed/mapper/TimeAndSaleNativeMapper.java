package com.dxfeed.mapper;

import com.dxfeed.NativeLibApi;
import com.dxfeed.event.market.TimeAndSale;
import org.graalvm.nativeimage.UnmanagedMemory;
import org.graalvm.nativeimage.c.struct.SizeOf;

public class TimeAndSaleNativeMapper extends BaseEventNativeMapper<TimeAndSale, NativeLibApi.TimeAndSaleNative> {

    @Override
    protected NativeLibApi.TimeAndSaleNative createNativeEvent(final TimeAndSale timeAndSale) {
        final NativeLibApi.TimeAndSaleNative timeAndSaleNative = UnmanagedMemory.calloc(SizeOf.get(NativeLibApi.TimeAndSaleNative.class));
        timeAndSaleNative.setEventType(NativeLibApi.EventsTypes.DXF_EVENT_TYPE_TIME_AND_SALE.getCValue());
        timeAndSaleNative.setEventFlag(timeAndSale.getEventFlags());
        timeAndSaleNative.setIndex(timeAndSale.getIndex());
        return timeAndSaleNative;
    }

    @Override
    protected void doDelete(final NativeLibApi.TimeAndSaleNative nativeEvent) {
        UnmanagedMemory.free(nativeEvent);
    }
}
