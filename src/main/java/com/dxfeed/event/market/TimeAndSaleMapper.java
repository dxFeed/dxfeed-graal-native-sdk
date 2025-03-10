package com.dxfeed.event.market;

import com.dxfeed.sdk.events.DxfgEventClazz;
import com.dxfeed.sdk.events.DxfgTimeAndSale;
import com.dxfeed.sdk.mappers.Mapper;
import org.graalvm.nativeimage.UnmanagedMemory;
import org.graalvm.nativeimage.c.struct.SizeOf;
import org.graalvm.nativeimage.c.type.CCharPointer;

public class TimeAndSaleMapper extends MarketEventMapper<TimeAndSale, DxfgTimeAndSale> {

  public TimeAndSaleMapper(
      final Mapper<String, CCharPointer> stringMapper
  ) {
    super(stringMapper);
  }

  @Override
  public DxfgTimeAndSale createNativeObject() {
    final DxfgTimeAndSale nObject = UnmanagedMemory.calloc(SizeOf.get(DxfgTimeAndSale.class));
    nObject.setClazz(DxfgEventClazz.DXFG_EVENT_TIME_AND_SALE.getCValue());
    return nObject;
  }

  @Override
  public void fillNative(final TimeAndSale jObject, final DxfgTimeAndSale nObject, boolean clean) {
    super.fillNative(jObject, nObject, clean);
    nObject.setEventFlags(jObject.getEventFlags());
    nObject.setIndex(jObject.getIndex());
    nObject.setTimeNanoPart(jObject.getTimeNanoPart());
    nObject.setExchangeCode(jObject.getExchangeCode());
    nObject.setPrice(jObject.getPrice());
    nObject.setSize(jObject.getSizeAsDouble());
    nObject.setBidPrice(jObject.getBidPrice());
    nObject.setAskPrice(jObject.getAskPrice());
    nObject.setExchangeSaleConditions(
        stringMapper.toNative(jObject.getExchangeSaleConditions())
    );
    nObject.setFlags(jObject.getFlags());
    nObject.setBuyer(stringMapper.toNative(jObject.getBuyer()));
    nObject.setSeller(stringMapper.toNative(jObject.getSeller()));
  }

  @Override
  public void cleanNative(final DxfgTimeAndSale nObject) {
    super.cleanNative(nObject);
    stringMapper.release(nObject.getExchangeSaleConditions());
    stringMapper.release(nObject.getBuyer());
    stringMapper.release(nObject.getSeller());
  }

  @Override
  protected TimeAndSale doToJava(final DxfgTimeAndSale nObject) {
    final TimeAndSale jObject = new TimeAndSale();
    this.fillJava(nObject, jObject);
    return jObject;
  }

  @Override
  public void fillJava(final DxfgTimeAndSale nObject, final TimeAndSale jObject) {
    super.fillJava(nObject, jObject);
    jObject.setEventFlags(nObject.getEventFlags());
    jObject.setIndex(nObject.getIndex());
    jObject.setTimeNanoPart(nObject.getTimeNanoPart());
    jObject.setExchangeCode(nObject.getExchangeCode());
    jObject.setPrice(nObject.getPrice());
    jObject.setSizeAsDouble(nObject.getSize());
    jObject.setBidPrice(nObject.getBidPrice());
    jObject.setAskPrice(nObject.getAskPrice());
    jObject.setExchangeSaleConditions(
        stringMapper.toJava(nObject.getExchangeSaleConditions())
    );
    jObject.setFlags(nObject.getFlags());
    jObject.setBuyer(stringMapper.toJava(nObject.getBuyer()));
    jObject.setSeller(stringMapper.toJava(nObject.getSeller()));
  }
}
