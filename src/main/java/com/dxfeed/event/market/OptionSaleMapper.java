package com.dxfeed.event.market;

import com.dxfeed.sdk.events.DxfgEventClazz;
import com.dxfeed.sdk.events.DxfgOptionSale;
import com.dxfeed.sdk.mappers.Mapper;
import org.graalvm.nativeimage.UnmanagedMemory;
import org.graalvm.nativeimage.c.struct.SizeOf;
import org.graalvm.nativeimage.c.type.CCharPointer;

public class OptionSaleMapper extends MarketEventMapper<OptionSale, DxfgOptionSale> {

  public OptionSaleMapper(
      final Mapper<String, CCharPointer> stringMapper
  ) {
    super(stringMapper);
  }

  @Override
  public DxfgOptionSale createNativeObject() {
    final DxfgOptionSale nativeObject = UnmanagedMemory.calloc(SizeOf.get(DxfgOptionSale.class));

    nativeObject.setClazz(DxfgEventClazz.DXFG_EVENT_OPTION_SALE.getCValue());

    return nativeObject;
  }

  @Override
  public void fillNative(final OptionSale javaObject, final DxfgOptionSale nativeObject,
      boolean clean) {
    super.fillNative(javaObject, nativeObject, clean);

    nativeObject.setEventFlags(javaObject.getEventFlags());
    nativeObject.setIndex(javaObject.getIndex());
    nativeObject.setTimeSequence(javaObject.getTimeSequence());
    nativeObject.setTimeNanoPart(javaObject.getTimeNanoPart());
    nativeObject.setExchangeCode(javaObject.getExchangeCode());
    nativeObject.setPrice(javaObject.getPrice());
    nativeObject.setSize(javaObject.getSize());
    nativeObject.setBidPrice(javaObject.getBidPrice());
    nativeObject.setAskPrice(javaObject.getAskPrice());
    nativeObject.setExchangeSaleConditions(
        stringMapper.toNative(javaObject.getExchangeSaleConditions())
    );
    nativeObject.setFlags(javaObject.getFlags());
    nativeObject.setUnderlyingPrice(javaObject.getUnderlyingPrice());
    nativeObject.setVolatility(javaObject.getVolatility());
    nativeObject.setDelta(javaObject.getDelta());
    nativeObject.setOptionSymbol(stringMapper.toNative((javaObject.getOptionSymbol())));
  }

  @Override
  public void cleanNative(final DxfgOptionSale nativeObject) {
    super.cleanNative(nativeObject);

    stringMapper.release(nativeObject.getExchangeSaleConditions());
    stringMapper.release(nativeObject.getOptionSymbol());
  }

  @Override
  protected OptionSale doToJava(final DxfgOptionSale nativeObject) {
    final OptionSale javaObject = new OptionSale();

    this.fillJava(nativeObject, javaObject);

    return javaObject;
  }

  @Override
  public void fillJava(final DxfgOptionSale nativeObject, final OptionSale javaObject) {
    super.fillJava(nativeObject, javaObject);

    javaObject.setEventFlags(nativeObject.getEventFlags());
    javaObject.setIndex(nativeObject.getIndex());
    javaObject.setTimeSequence(nativeObject.getTimeSequence());
    javaObject.setTimeNanoPart(nativeObject.getTimeNanoPart());
    javaObject.setExchangeCode(nativeObject.getExchangeCode());
    javaObject.setPrice(nativeObject.getPrice());
    javaObject.setSize(nativeObject.getSize());
    javaObject.setBidPrice(nativeObject.getBidPrice());
    javaObject.setAskPrice(nativeObject.getAskPrice());
    javaObject.setExchangeSaleConditions(
        stringMapper.toJava(nativeObject.getExchangeSaleConditions())
    );
    javaObject.setFlags(nativeObject.getFlags());
    javaObject.setUnderlyingPrice(nativeObject.getUnderlyingPrice());
    javaObject.setVolatility(nativeObject.getVolatility());
    javaObject.setDelta(nativeObject.getDelta());
    javaObject.setOptionSymbol(stringMapper.toJava((nativeObject.getOptionSymbol())));
  }
}
