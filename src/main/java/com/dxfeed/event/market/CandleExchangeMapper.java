package com.dxfeed.event.market;

import com.dxfeed.api.events.DxfgCandleExchange;
import com.dxfeed.event.candle.CandleExchange;
import org.graalvm.nativeimage.UnmanagedMemory;
import org.graalvm.nativeimage.c.struct.SizeOf;
import org.graalvm.word.WordFactory;

public class CandleExchangeMapper extends Mapper<CandleExchange, DxfgCandleExchange> {

  @Override
  public DxfgCandleExchange toNativeObject(final CandleExchange jObject) {
    if (jObject == null) {
      return WordFactory.nullPointer();
    }
    final DxfgCandleExchange nObject = UnmanagedMemory.calloc(SizeOf.get(DxfgCandleExchange.class));
    fillNativeObject(jObject, nObject);
    return nObject;
  }

  @Override
  public void fillNativeObject(final CandleExchange jObject, final DxfgCandleExchange nObject) {
    nObject.setExchangeCode(jObject.getExchangeCode());
  }

  @Override
  protected void cleanNativeObject(final DxfgCandleExchange nObject) {
    // nothing
  }

  @Override
  public CandleExchange toJavaObject(final DxfgCandleExchange nObject) {
    throw new IllegalStateException();
  }

  @Override
  public void fillJavaObject(final DxfgCandleExchange nObject, final CandleExchange jObject) {
    throw new IllegalStateException();
  }
}
