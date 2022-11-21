package com.dxfeed.event.market;

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
  public DxfgCandlePeriod toNativeObject(final CandlePeriod jObject) {
    if (jObject == null) {
      return WordFactory.nullPointer();
    }
    final DxfgCandlePeriod nObject = UnmanagedMemory.calloc(SizeOf.get(DxfgCandlePeriod.class));
    fillNativeObject(jObject, nObject);
    return nObject;
  }

  @Override
  public void fillNativeObject(final CandlePeriod jObject, final DxfgCandlePeriod nObject) {
    cleanNativeObject(nObject);
    nObject.setPeriodIntervalMillis(jObject.getPeriodIntervalMillis());
    nObject.setValueCandlePeriod(jObject.getValue());
    nObject.setType(DxfgCandleType.valueOf(jObject.getType().name()).getCValue());
    nObject.setString(stringMapper.toNativeObject(jObject.toString()));
  }

  @Override
  protected void cleanNativeObject(final DxfgCandlePeriod nObject) {
    stringMapper.release(nObject.getString());
  }

  @Override
  public CandlePeriod toJavaObject(final DxfgCandlePeriod nObject) {
    throw new IllegalStateException();
  }

  @Override
  public void fillJavaObject(final DxfgCandlePeriod nObject, final CandlePeriod jObject) {
    throw new IllegalStateException();
  }
}
