package com.dxfeed.event.market;

import com.dxfeed.api.events.DxfgEventClazz;
import com.dxfeed.api.events.DxfgUnderlying;
import com.dxfeed.event.option.Underlying;
import org.graalvm.nativeimage.UnmanagedMemory;
import org.graalvm.nativeimage.c.struct.SizeOf;
import org.graalvm.nativeimage.c.type.CCharPointer;

public class UnderlyingMapper extends MarketEventMapper<Underlying, DxfgUnderlying> {

  public UnderlyingMapper(
      final Mapper<String, CCharPointer> stringMapperForMarketEvent
  ) {
    super(stringMapperForMarketEvent);
  }

  @Override
  public DxfgUnderlying createNativeObject() {
    final DxfgUnderlying nObject = UnmanagedMemory.calloc(SizeOf.get(DxfgUnderlying.class));
    nObject.setClazz(DxfgEventClazz.DXFG_EVENT_UNDERLYING.getCValue());
    return nObject;
  }

  @Override
  public void fillNativeObject(final Underlying jObject, final DxfgUnderlying nObject) {
    super.fillNativeObject(jObject, nObject);
    nObject.setEventFlags(jObject.getEventFlags());
    nObject.setIndex(jObject.getIndex());
    nObject.setVolatility(jObject.getVolatility());
    nObject.setBackVolatility(jObject.getBackVolatility());
    nObject.setCallVolume(jObject.getCallVolume());
    nObject.setPutVolume(jObject.getPutVolume());
    nObject.setPutCallRatio(jObject.getPutCallRatio());
  }

  @Override
  protected void cleanNativeObject(final DxfgUnderlying nObject) {
    super.cleanNativeObject(nObject);
  }

  @Override
  public Underlying toJavaObject(final DxfgUnderlying nObject) {
    final Underlying jObject = new Underlying();
    fillJavaObject(nObject, jObject);
    return jObject;
  }

  @Override
  public void fillJavaObject(final DxfgUnderlying nObject, final Underlying jObject) {
    super.fillJavaObject(nObject, jObject);
    jObject.setEventFlags(nObject.getEventFlags());
    jObject.setIndex(nObject.getIndex());
    jObject.setVolatility(nObject.getVolatility());
    jObject.setBackVolatility(nObject.getBackVolatility());
    jObject.setCallVolume(nObject.getCallVolume());
    jObject.setPutVolume(nObject.getPutVolume());
    jObject.setPutCallRatio(nObject.getPutCallRatio());
  }
}
