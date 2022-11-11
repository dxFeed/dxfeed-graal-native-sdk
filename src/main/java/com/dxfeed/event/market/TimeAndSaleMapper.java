package com.dxfeed.event.market;

import com.dxfeed.api.events.DxfgEventKind;
import com.dxfeed.api.events.DxfgTimeAndSale;
import org.graalvm.nativeimage.c.struct.SizeOf;
import org.graalvm.nativeimage.c.type.CCharPointer;

public class TimeAndSaleMapper extends Mapper<TimeAndSale, DxfgTimeAndSale> {

  private final MarketEventMapper marketEventMapper;
  private final Mapper<String, CCharPointer> stringMapper;

  public TimeAndSaleMapper(
      final MarketEventMapper marketEventMapper,
      final Mapper<String, CCharPointer> stringMapper
  ) {
    this.marketEventMapper = marketEventMapper;
    this.stringMapper = stringMapper;
  }

  @Override
  protected int size() {
    return SizeOf.get(DxfgTimeAndSale.class);
  }

  @Override
  protected void fillNativeObject(final DxfgTimeAndSale nObject, final TimeAndSale jObject) {
    nObject.setKind(DxfgEventKind.DXFG_EVENT_TYPE_TIME_AND_SALE.getCValue());
    this.marketEventMapper.fillNativeObject(nObject, jObject);
    nObject.setEventFlags(jObject.getEventFlags());
    nObject.setIndex(jObject.getIndex());
    nObject.setTimeNanoPart(jObject.getTimeNanoPart());
    nObject.setExchangeCode(jObject.getExchangeCode());
    nObject.setPrice(jObject.getPrice());
    nObject.setSize(jObject.getSizeAsDouble());
    nObject.setBidPrice(jObject.getBidPrice());
    nObject.setAskPrice(jObject.getAskPrice());
    nObject.setExchangeSaleConditions(
        this.stringMapper.nativeObject(jObject.getExchangeSaleConditions())
    );
    nObject.setFlags(jObject.getFlags());
    nObject.setBuyer(this.stringMapper.nativeObject(jObject.getBuyer()));
    nObject.setSeller(this.stringMapper.nativeObject(jObject.getSeller()));
  }

  @Override
  protected void cleanNativeObject(final DxfgTimeAndSale nObject) {
    this.marketEventMapper.cleanNativeObject(nObject);
    this.stringMapper.delete(nObject.getExchangeSaleConditions());
    this.stringMapper.delete(nObject.getBuyer());
    this.stringMapper.delete(nObject.getSeller());
  }
}
