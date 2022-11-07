package com.dxfeed.event.market;

import com.dxfeed.api.events.DxfgEventKind;
import com.dxfeed.api.events.DxfgTimeAndSale;
import org.graalvm.nativeimage.UnmanagedMemory;
import org.graalvm.nativeimage.c.struct.SizeOf;

public class TimeAndSaleNativeMapper extends MarketEventNativeMapper<TimeAndSale, DxfgTimeAndSale> {

  @Override
  protected DxfgTimeAndSale createNativeEvent(final TimeAndSale timeAndSale) {
    final DxfgTimeAndSale dxfgTimeAndSale = UnmanagedMemory.calloc(
        SizeOf.get(DxfgTimeAndSale.class));
    dxfgTimeAndSale.setKind(DxfgEventKind.DXFG_EVENT_TYPE_TIME_AND_SALE.getCValue());
    dxfgTimeAndSale.setEventFlags(timeAndSale.getEventFlags());
    dxfgTimeAndSale.setIndex(timeAndSale.getIndex());
    dxfgTimeAndSale.setTimeNanoPart(timeAndSale.getTimeNanoPart());
    dxfgTimeAndSale.setExchangeCode(timeAndSale.getExchangeCode());
    dxfgTimeAndSale.setPrice(timeAndSale.getPrice());
    dxfgTimeAndSale.setSize(timeAndSale.getSizeAsDouble());
    dxfgTimeAndSale.setBidPrice(timeAndSale.getBidPrice());
    dxfgTimeAndSale.setAskPrice(timeAndSale.getAskPrice());
    dxfgTimeAndSale.setExchangeSaleConditions(
        toCString(timeAndSale.getExchangeSaleConditions()));
    dxfgTimeAndSale.setFlags(timeAndSale.getFlags());
    dxfgTimeAndSale.setBuyer(toCString(timeAndSale.getBuyer()));
    dxfgTimeAndSale.setSeller(toCString(timeAndSale.getSeller()));
    return dxfgTimeAndSale;
  }

  @Override
  protected void doDelete(final DxfgTimeAndSale dxfgTimeAndSale) {
    UnmanagedMemory.free(dxfgTimeAndSale.getExchangeSaleConditions());
    UnmanagedMemory.free(dxfgTimeAndSale.getBuyer());
    UnmanagedMemory.free(dxfgTimeAndSale.getSeller());
    UnmanagedMemory.free(dxfgTimeAndSale);
  }
}
