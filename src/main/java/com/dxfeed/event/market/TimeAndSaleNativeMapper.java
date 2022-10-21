package com.dxfeed.event.market;

import com.dxfeed.api.UtilsNative;
import com.dxfeed.api.events.EventTypesNative;
import com.dxfeed.api.events.TimeAndSaleNative;
import org.graalvm.nativeimage.UnmanagedMemory;
import org.graalvm.nativeimage.c.struct.SizeOf;

public class TimeAndSaleNativeMapper extends BaseEventNativeMapper<TimeAndSale, TimeAndSaleNative> {
    @Override
    protected TimeAndSaleNative createNativeEvent(TimeAndSale timeAndSale) {
        TimeAndSaleNative timeAndSaleNative = UnmanagedMemory.calloc(SizeOf.get(TimeAndSaleNative.class));
        timeAndSaleNative.setEventType(EventTypesNative.DXFG_EVENT_TYPE_TIME_AND_SALE.getCValue());
        timeAndSaleNative.setEventTime(timeAndSale.getEventTime());
        timeAndSaleNative.setEventFlags(timeAndSale.getEventFlags());
        timeAndSaleNative.setIndex(timeAndSale.getIndex());
        timeAndSaleNative.setTimeNanoPart(timeAndSale.getTimeNanoPart());
        timeAndSaleNative.setExchangeCode(timeAndSale.getExchangeCode());
        timeAndSaleNative.setPrice(timeAndSale.getPrice());
        timeAndSaleNative.setSize(timeAndSale.getSizeAsDouble());
        timeAndSaleNative.setBidPrice(timeAndSale.getBidPrice());
        timeAndSaleNative.setAskPrice(timeAndSale.getAskPrice());
        timeAndSaleNative.setExchangeSaleCondition(UtilsNative.createCString(timeAndSale.getExchangeSaleConditions()));
        timeAndSaleNative.setFlags(timeAndSale.getFlags());
        timeAndSaleNative.setBuyer(UtilsNative.createCString(timeAndSale.getBuyer()));
        timeAndSaleNative.setSeller(UtilsNative.createCString(timeAndSale.getSeller()));
        return timeAndSaleNative;
    }

    @Override
    protected void doDelete(TimeAndSaleNative timeAndSaleNative) {
        UnmanagedMemory.free(timeAndSaleNative.getExchangeSaleCondition());
        UnmanagedMemory.free(timeAndSaleNative.getBuyer());
        UnmanagedMemory.free(timeAndSaleNative.getSeller());
        UnmanagedMemory.free(timeAndSaleNative);
    }
}
