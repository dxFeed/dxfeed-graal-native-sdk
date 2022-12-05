package com.dxfeed.event.market;

import com.dxfeed.api.Mapper;
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
  public void fillNative(final Underlying jObject, final DxfgUnderlying nObject) {
    super.fillNative(jObject, nObject);
    nObject.setEventFlags(jObject.getEventFlags());
    nObject.setIndex(jObject.getIndex());
    nObject.setVolatility(jObject.getVolatility());
    nObject.setBackVolatility(jObject.getBackVolatility());
    nObject.setCallVolume(jObject.getCallVolume());
    nObject.setPutVolume(jObject.getPutVolume());
    nObject.setPutCallRatio(jObject.getPutCallRatio());
  }

  @Override
  public void cleanNative(final DxfgUnderlying nObject) {
    super.cleanNative(nObject);
  }

  @Override
  public Underlying toJava(final DxfgUnderlying nObject) {
    final Underlying jObject = new Underlying();
    this.fillJava(nObject, jObject);
    return jObject;
  }

  @Override
  public void fillJava(final DxfgUnderlying nObject, final Underlying jObject) {
    super.fillJava(nObject, jObject);
    jObject.setEventFlags(nObject.getEventFlags());
    jObject.setIndex(nObject.getIndex());
    jObject.setVolatility(nObject.getVolatility());
    jObject.setBackVolatility(nObject.getBackVolatility());
    jObject.setCallVolume(nObject.getCallVolume());
    jObject.setPutVolume(nObject.getPutVolume());
    jObject.setPutCallRatio(nObject.getPutCallRatio());
  }
}
