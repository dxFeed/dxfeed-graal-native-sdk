package com.dxfeed.event.market;

import com.dxfeed.api.events.DxfgEventKind;
import com.dxfeed.api.events.DxfgTimeAndSale;
import org.graalvm.nativeimage.c.struct.SizeOf;

public class TimeAndSaleMapper extends MarketEventMapper<TimeAndSale, DxfgTimeAndSale> {

  public TimeAndSaleMapper(final StringMapper stringMapper) {
    super(stringMapper);
  }

  @Override
  protected int size() {
    return SizeOf.get(DxfgTimeAndSale.class);
  }

  @Override
  protected void doFillNativeObject(final DxfgTimeAndSale nObject, final TimeAndSale jObject) {
    nObject.setKind(DxfgEventKind.DXFG_EVENT_TYPE_TIME_AND_SALE.getCValue());
    nObject.setEventFlags(jObject.getEventFlags());
    nObject.setIndex(jObject.getIndex());
    nObject.setTimeNanoPart(jObject.getTimeNanoPart());
    nObject.setExchangeCode(jObject.getExchangeCode());
    nObject.setPrice(jObject.getPrice());
    nObject.setSize(jObject.getSizeAsDouble());
    nObject.setBidPrice(jObject.getBidPrice());
    nObject.setAskPrice(jObject.getAskPrice());
    nObject.setExchangeSaleConditions(
        super.stringMapper.nativeObject(jObject.getExchangeSaleConditions())
    );
    nObject.setFlags(jObject.getFlags());
    nObject.setBuyer(super.stringMapper.nativeObject(jObject.getBuyer()));
    nObject.setSeller(super.stringMapper.nativeObject(jObject.getSeller()));
  }

  @Override
  protected void doCleanNativeObject(final DxfgTimeAndSale nObject) {
    super.stringMapper.delete(nObject.getExchangeSaleConditions());
    super.stringMapper.delete(nObject.getBuyer());
    super.stringMapper.delete(nObject.getSeller());
  }
}
