package com.dxfeed.event.market;

import com.dxfeed.api.Mapper;
import com.dxfeed.api.events.DxfgCandlePriceLevel;
import com.dxfeed.event.candle.CandlePriceLevel;
import org.graalvm.nativeimage.UnmanagedMemory;
import org.graalvm.nativeimage.c.struct.SizeOf;
import org.graalvm.word.WordFactory;

public class CandlePriceLevelMapper extends Mapper<CandlePriceLevel, DxfgCandlePriceLevel> {

  @Override
  public DxfgCandlePriceLevel toNative(final CandlePriceLevel jObject) {
    if (jObject == null) {
      return WordFactory.nullPointer();
    }
    final DxfgCandlePriceLevel nObject = UnmanagedMemory.calloc(
        SizeOf.get(DxfgCandlePriceLevel.class));
    fillNative(jObject, nObject);
    return nObject;
  }

  @Override
  public void fillNative(
      final CandlePriceLevel jObject, final DxfgCandlePriceLevel nObject
  ) {
    nObject.setValuePriceLevel(jObject.getValue());
  }

  @Override
  public void cleanNative(final DxfgCandlePriceLevel nObject) {
    // nothing
  }

  @Override
  public CandlePriceLevel toJava(final DxfgCandlePriceLevel nObject) {
    throw new IllegalStateException();
  }

  @Override
  public void fillJava(final DxfgCandlePriceLevel nObject, final CandlePriceLevel jObject) {
    throw new IllegalStateException();
  }
}
