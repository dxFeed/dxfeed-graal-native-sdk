package com.dxfeed.event.market;

import com.dxfeed.api.events.DxfgEventClazz;
import com.dxfeed.api.events.DxfgOptionSale;
import com.dxfeed.api.maper.Mapper;
import org.graalvm.nativeimage.UnmanagedMemory;
import org.graalvm.nativeimage.c.struct.SizeOf;
import org.graalvm.nativeimage.c.type.CCharPointer;

public class OptionSaleMapper extends MarketEventMapper<OptionSale, DxfgOptionSale> {

  private final Mapper<String, CCharPointer> stringMapper;

  public OptionSaleMapper(
      final Mapper<String, CCharPointer> stringMapperForMarketEvent,
      final Mapper<String, CCharPointer> stringMapper
  ) {
    super(stringMapperForMarketEvent);
    this.stringMapper = stringMapper;
  }

  @Override
  public DxfgOptionSale createNativeObject() {
    final DxfgOptionSale nObject = UnmanagedMemory.calloc(SizeOf.get(DxfgOptionSale.class));
    nObject.setClazz(DxfgEventClazz.DXFG_EVENT_OPTION_SALE.getCValue());
    return nObject;
  }

  @Override
  public void fillNative(final OptionSale jObject, final DxfgOptionSale nObject) {
    super.fillNative(jObject, nObject);
    nObject.setEventFlags(jObject.getEventFlags());
    nObject.setIndex(jObject.getIndex());
    nObject.setTimeSequence(jObject.getTimeSequence());
    nObject.setTimeNanoPart(jObject.getTimeNanoPart());
    nObject.setExchangeCode(jObject.getExchangeCode());
    nObject.setPrice(jObject.getPrice());
    nObject.setSize(jObject.getSize());
    nObject.setBidPrice(jObject.getBidPrice());
    nObject.setAskPrice(jObject.getAskPrice());
    nObject.setExchangeSaleConditions(
        this.stringMapper.toNative(jObject.getExchangeSaleConditions())
    );
    nObject.setFlags(jObject.getFlags());
    nObject.setUnderlyingPrice(jObject.getUnderlyingPrice());
    nObject.setVolatility(jObject.getVolatility());
    nObject.setDelta(jObject.getDelta());
    nObject.setOptionSymbol(this.stringMapper.toNative((jObject.getOptionSymbol())));
  }

  @Override
  public void cleanNative(final DxfgOptionSale nObject) {
    super.cleanNative(nObject);
    this.stringMapper.release(nObject.getExchangeSaleConditions());
    this.stringMapper.release(nObject.getOptionSymbol());
  }

  @Override
  protected OptionSale doToJava(final DxfgOptionSale nObject) {
    final OptionSale jObject = new OptionSale();
    this.fillJava(nObject, jObject);
    return jObject;
  }

  @Override
  public void fillJava(final DxfgOptionSale nObject, final OptionSale jObject) {
    super.fillJava(nObject, jObject);
    jObject.setEventFlags(nObject.getEventFlags());
    jObject.setIndex(nObject.getIndex());
    jObject.setTimeSequence(nObject.getTimeSequence());
    jObject.setTimeNanoPart(nObject.getTimeNanoPart());
    jObject.setExchangeCode(nObject.getExchangeCode());
    jObject.setPrice(nObject.getPrice());
    jObject.setSize(nObject.getSize());
    jObject.setBidPrice(nObject.getBidPrice());
    jObject.setAskPrice(nObject.getAskPrice());
    jObject.setExchangeSaleConditions(
        this.stringMapper.toJava(nObject.getExchangeSaleConditions())
    );
    jObject.setFlags(nObject.getFlags());
    jObject.setUnderlyingPrice(nObject.getUnderlyingPrice());
    jObject.setVolatility(nObject.getVolatility());
    jObject.setDelta(nObject.getDelta());
    jObject.setOptionSymbol(this.stringMapper.toJava((nObject.getOptionSymbol())));
  }
}
