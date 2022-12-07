package com.dxfeed.event.market;

import com.dxfeed.api.maper.Mapper;
import com.dxfeed.api.events.DxfgEventClazz;
import com.dxfeed.api.events.DxfgOrder;
import org.graalvm.nativeimage.UnmanagedMemory;
import org.graalvm.nativeimage.c.struct.SizeOf;
import org.graalvm.nativeimage.c.type.CCharPointer;

public class OrderMapper extends OrderAbstractMapper<Order, DxfgOrder> {

  private final Mapper<String, CCharPointer> stringMapper;

  public OrderMapper(
      final Mapper<String, CCharPointer> stringMapperForMarketEvent,
      final Mapper<String, CCharPointer> stringMapper
  ) {
    super(stringMapperForMarketEvent);
    this.stringMapper = stringMapper;
  }

  @Override
  public DxfgOrder createNativeObject() {
    final DxfgOrder nObject = UnmanagedMemory.calloc(SizeOf.get(DxfgOrder.class));
    nObject.setClazz(DxfgEventClazz.DXFG_EVENT_ORDER.getCValue());
    return nObject;
  }

  @Override
  public void fillNative(final Order jObject, final DxfgOrder nObject) {
    super.fillNative(jObject, nObject);
    nObject.setMarketMaker(this.stringMapper.toNative(jObject.getMarketMaker()));
  }

  @Override
  public void cleanNative(final DxfgOrder nObject) {
    super.cleanNative(nObject);
  }

  @Override
  protected Order doToJava(final DxfgOrder nObject) {
    final Order jObject = new Order();
    this.fillJava(nObject, jObject);
    return jObject;
  }

  @Override
  public void fillJava(final DxfgOrder nObject, final Order jObject) {
    super.fillJava(nObject, jObject);
    jObject.setMarketMaker(this.stringMapper.toJava(nObject.getMarketMaker()));
  }
}
