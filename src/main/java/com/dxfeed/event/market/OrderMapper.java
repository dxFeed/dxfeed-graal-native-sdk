package com.dxfeed.event.market;

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
  public void fillNativeObject(final Order jObject, final DxfgOrder nObject) {
    super.fillNativeObject(jObject, nObject);
    nObject.setMarketMaker(this.stringMapper.toNativeObject(jObject.getMarketMaker()));
  }

  @Override
  protected void cleanNativeObject(final DxfgOrder nObject) {
    super.cleanNativeObject(nObject);
  }

  @Override
  public Order toJavaObject(final DxfgOrder nObject) {
    final Order jObject = new Order();
    fillJavaObject(nObject, jObject);
    return jObject;
  }

  @Override
  public void fillJavaObject(final DxfgOrder nObject, final Order jObject) {
    super.fillJavaObject(nObject, jObject);
    jObject.setMarketMaker(this.stringMapper.toJavaObject(nObject.getMarketMaker()));
  }
}
