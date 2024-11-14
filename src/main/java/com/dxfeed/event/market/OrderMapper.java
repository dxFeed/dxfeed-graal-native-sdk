package com.dxfeed.event.market;

import com.dxfeed.sdk.events.DxfgEventClazz;
import com.dxfeed.sdk.events.DxfgOrder;
import com.dxfeed.sdk.mappers.Mapper;
import org.graalvm.nativeimage.UnmanagedMemory;
import org.graalvm.nativeimage.c.struct.SizeOf;
import org.graalvm.nativeimage.c.type.CCharPointer;

public class OrderMapper<T extends Order, V extends DxfgOrder> extends OrderAbstractMapper<T, V> {

  private final Mapper<String, CCharPointer> stringMapper;

  public OrderMapper(
      final Mapper<String, CCharPointer> stringMapperForMarketEvent,
      final Mapper<String, CCharPointer> stringMapper
  ) {
    super(stringMapperForMarketEvent);
    this.stringMapper = stringMapper;
  }

  @Override
  public V createNativeObject() {
    final V nObject = UnmanagedMemory.calloc(SizeOf.get(DxfgOrder.class));
    nObject.setClazz(DxfgEventClazz.DXFG_EVENT_ORDER.getCValue());
    return nObject;
  }

  @Override
  public void fillNative(final T jObject, final V nObject, boolean clean) {
    super.fillNative(jObject, nObject, clean);
    nObject.setMarketMaker(this.stringMapper.toNative(jObject.getMarketMaker()));
  }

  @Override
  public void cleanNative(final V nObject) {
    super.cleanNative(nObject);
    this.stringMapper.release(nObject.getMarketMaker());
  }

  @Override
  protected T doToJava(final V nObject) {
    final T jObject = (T) new Order();
    this.fillJava(nObject, jObject);
    return jObject;
  }

  @Override
  public void fillJava(final V nObject, final T jObject) {
    super.fillJava(nObject, jObject);
    jObject.setMarketMaker(this.stringMapper.toJava(nObject.getMarketMaker()));
  }
}
