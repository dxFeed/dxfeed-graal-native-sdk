package com.dxfeed.event.market;

import com.dxfeed.api.events.DxfgCandlePriceLevel;
import com.dxfeed.event.candle.CandlePriceLevel;
import org.graalvm.nativeimage.c.struct.SizeOf;

public class CandlePriceLevelMapper extends Mapper<CandlePriceLevel, DxfgCandlePriceLevel> {

  @Override
  protected int size() {
    return SizeOf.get(DxfgCandlePriceLevel.class);
  }

  @Override
  protected void fillNativeObject(
      final DxfgCandlePriceLevel nObject,
      final CandlePriceLevel jObject
  ) {
    nObject.setValuePriceLevel(jObject.getValue());
  }

  @Override
  protected void cleanNativeObject(final DxfgCandlePriceLevel nObject) {
    // nothing
  }
}
