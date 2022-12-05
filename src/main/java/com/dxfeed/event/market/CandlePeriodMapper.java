package com.dxfeed.event.market;

import com.dxfeed.api.Mapper;
import com.dxfeed.api.events.DxfgCandlePeriod;
import com.dxfeed.api.events.DxfgCandleType;
import com.dxfeed.event.candle.CandlePeriod;
import org.graalvm.nativeimage.UnmanagedMemory;
import org.graalvm.nativeimage.c.struct.SizeOf;
import org.graalvm.nativeimage.c.type.CCharPointer;
import org.graalvm.word.WordFactory;

public class CandlePeriodMapper extends Mapper<CandlePeriod, DxfgCandlePeriod> {

  protected final Mapper<String, CCharPointer> stringMapper;

  public CandlePeriodMapper(final Mapper<String, CCharPointer> stringMapper) {
    this.stringMapper = stringMapper;
  }

  @Override
  public DxfgCandlePeriod toNative(final CandlePeriod jObject) {
    if (jObject == null) {
      return WordFactory.nullPointer();
    }
    final DxfgCandlePeriod nObject = UnmanagedMemory.calloc(SizeOf.get(DxfgCandlePeriod.class));
    fillNative(jObject, nObject);
    return nObject;
  }

  @Override
  public void fillNative(final CandlePeriod jObject, final DxfgCandlePeriod nObject) {
    cleanNative(nObject);
    nObject.setPeriodIntervalMillis(jObject.getPeriodIntervalMillis());
    nObject.setValueCandlePeriod(jObject.getValue());
    nObject.setType(DxfgCandleType.valueOf(jObject.getType().name()).getCValue());
    nObject.setString(stringMapper.toNative(jObject.toString()));
  }

  @Override
  public void cleanNative(final DxfgCandlePeriod nObject) {
    stringMapper.release(nObject.getString());
  }

  @Override
  public CandlePeriod toJava(final DxfgCandlePeriod nObject) {
    throw new IllegalStateException();
  }

  @Override
  public void fillJava(final DxfgCandlePeriod nObject, final CandlePeriod jObject) {
    throw new IllegalStateException();
  }
}
