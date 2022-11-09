package com.dxfeed.event.market;

import com.dxfeed.api.events.DxfgCandlePeriod;
import com.dxfeed.api.events.DxfgCandleType;
import com.dxfeed.event.candle.CandlePeriod;
import org.graalvm.nativeimage.c.struct.SizeOf;

public class CandlePeriodMapper extends Mapper<CandlePeriod, DxfgCandlePeriod> {

  protected final StringMapper stringMapper;

  public CandlePeriodMapper(final StringMapper stringMapper) {
    this.stringMapper = stringMapper;
  }

  @Override
  protected int size() {
    return SizeOf.get(DxfgCandlePeriod.class);
  }

  @Override
  protected void fillNativeObject(final DxfgCandlePeriod nObject, final CandlePeriod jObject) {
    nObject.setPeriodIntervalMillis(jObject.getPeriodIntervalMillis());
    nObject.setValueCandlePeriod(jObject.getValue());
    nObject.setType(DxfgCandleType.valueOf(jObject.getType().name()).getCValue());
    nObject.setString(stringMapper.nativeObject(jObject.toString()));
  }

  @Override
  protected void cleanNativeObject(final DxfgCandlePeriod nObject) {
    stringMapper.delete(nObject.getString());
  }
}
