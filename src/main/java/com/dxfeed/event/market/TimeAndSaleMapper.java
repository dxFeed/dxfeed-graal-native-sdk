package com.dxfeed.event.market;

import com.dxfeed.api.events.DxfgEventClazz;
import com.dxfeed.api.events.DxfgTimeAndSale;
import org.graalvm.nativeimage.UnmanagedMemory;
import org.graalvm.nativeimage.c.struct.SizeOf;
import org.graalvm.nativeimage.c.type.CCharPointer;

public class TimeAndSaleMapper extends MarketEventMapper<TimeAndSale, DxfgTimeAndSale> {

  private final Mapper<String, CCharPointer> stringMapper;

  public TimeAndSaleMapper(
      final Mapper<String, CCharPointer> stringMapperForMarketEvent,
      final Mapper<String, CCharPointer> stringMapper
  ) {
    super(stringMapperForMarketEvent);
    this.stringMapper = stringMapper;
  }

  @Override
  public DxfgTimeAndSale createNativeObject() {
    final DxfgTimeAndSale nObject = UnmanagedMemory.calloc(SizeOf.get(DxfgTimeAndSale.class));
    nObject.setClazz(DxfgEventClazz.DXFG_EVENT_TIME_AND_SALE.getCValue());
    return nObject;
  }

  @Override
  public void fillNativeObject(final TimeAndSale jObject, final DxfgTimeAndSale nObject) {
    super.fillNativeObject(jObject, nObject);
    nObject.setEventFlags(jObject.getEventFlags());
    nObject.setIndex(jObject.getIndex());
    nObject.setTimeNanoPart(jObject.getTimeNanoPart());
    nObject.setExchangeCode(jObject.getExchangeCode());
    nObject.setPrice(jObject.getPrice());
    nObject.setSize(jObject.getSizeAsDouble());
    nObject.setBidPrice(jObject.getBidPrice());
    nObject.setAskPrice(jObject.getAskPrice());
    nObject.setExchangeSaleConditions(
        this.stringMapper.toNativeObject(jObject.getExchangeSaleConditions())
    );
    nObject.setFlags(jObject.getFlags());
    nObject.setBuyer(this.stringMapper.toNativeObject(jObject.getBuyer()));
    nObject.setSeller(this.stringMapper.toNativeObject(jObject.getSeller()));
  }

  @Override
  protected void cleanNativeObject(final DxfgTimeAndSale nObject) {
    super.cleanNativeObject(nObject);
    this.stringMapper.release(nObject.getExchangeSaleConditions());
    this.stringMapper.release(nObject.getBuyer());
    this.stringMapper.release(nObject.getSeller());
  }

  @Override
  public TimeAndSale toJavaObject(final DxfgTimeAndSale nObject) {
    final TimeAndSale jObject = new TimeAndSale();
    fillJavaObject(nObject, jObject);
    return jObject;
  }

  @Override
  public void fillJavaObject(final DxfgTimeAndSale nObject, final TimeAndSale jObject) {
    super.fillJavaObject(nObject, jObject);
    jObject.setEventFlags(nObject.getEventFlags());
    jObject.setIndex(nObject.getIndex());
    jObject.setTimeNanoPart(nObject.getTimeNanoPart());
    jObject.setExchangeCode(nObject.getExchangeCode());
    jObject.setPrice(nObject.getPrice());
    jObject.setSizeAsDouble(nObject.getSize());
    jObject.setBidPrice(nObject.getBidPrice());
    jObject.setAskPrice(nObject.getAskPrice());
    jObject.setExchangeSaleConditions(
        this.stringMapper.toJavaObject(nObject.getExchangeSaleConditions())
    );
    jObject.setFlags(nObject.getFlags());
    jObject.setBuyer(this.stringMapper.toJavaObject(nObject.getBuyer()));
    jObject.setSeller(this.stringMapper.toJavaObject(nObject.getSeller()));
  }
}
