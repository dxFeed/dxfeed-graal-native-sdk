package com.dxfeed.event.market;

import com.dxfeed.sdk.events.DxfgEventClazz;
import com.dxfeed.sdk.events.DxfgQuote;
import com.dxfeed.sdk.maper.Mapper;
import org.graalvm.nativeimage.UnmanagedMemory;
import org.graalvm.nativeimage.c.struct.SizeOf;
import org.graalvm.nativeimage.c.type.CCharPointer;

public class QuoteMapper extends MarketEventMapper<Quote, DxfgQuote> {

  public QuoteMapper(
      final Mapper<String, CCharPointer> stringMapperForMarketEvent
  ) {
    super(stringMapperForMarketEvent);
  }

  @Override
  public DxfgQuote createNativeObject() {
    final DxfgQuote nObject = UnmanagedMemory.calloc(SizeOf.get(DxfgQuote.class));
    nObject.setClazz(DxfgEventClazz.DXFG_EVENT_QUOTE.getCValue());
    return nObject;
  }

  @Override
  public void fillNative(final Quote jObject, final DxfgQuote nObject) {
    super.fillNative(jObject, nObject);
    nObject.setTimeMillisSequence(jObject.getTimeMillisSequence());
    nObject.setTimeNanoPart(jObject.getTimeNanoPart());
    nObject.setBidTime(jObject.getBidTime());
    nObject.setBidExchangeCode(jObject.getBidExchangeCode());
    nObject.setBidPrice(jObject.getBidPrice());
    nObject.setBidSize(jObject.getBidSizeAsDouble());
    nObject.setAskTime(jObject.getAskTime());
    nObject.setAskExchangeCode(jObject.getAskExchangeCode());
    nObject.setAskPrice(jObject.getAskPrice());
    nObject.setAskSize(jObject.getAskSizeAsDouble());
  }

  @Override
  public void cleanNative(final DxfgQuote nObject) {
    super.cleanNative(nObject);
  }

  @Override
  protected Quote doToJava(final DxfgQuote nObject) {
    final Quote jObject = new Quote();
    this.fillJava(nObject, jObject);
    return jObject;
  }

  @Override
  public void fillJava(final DxfgQuote nObject, final Quote jObject) {
    super.fillJava(nObject, jObject);
    jObject.setTimeMillisSequence(nObject.getTimeMillisSequence());
    jObject.setTimeNanoPart(nObject.getTimeNanoPart());
    jObject.setBidTime(nObject.getBidTime());
    jObject.setBidExchangeCode(nObject.getBidExchangeCode());
    jObject.setBidPrice(nObject.getBidPrice());
    jObject.setBidSizeAsDouble(nObject.getBidSize());
    jObject.setAskTime(nObject.getAskTime());
    jObject.setAskExchangeCode(nObject.getAskExchangeCode());
    jObject.setAskPrice(nObject.getAskPrice());
    jObject.setAskSizeAsDouble(nObject.getAskSize());
  }
}
