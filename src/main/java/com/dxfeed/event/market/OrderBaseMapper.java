package com.dxfeed.event.market;

import com.dxfeed.api.events.DxfgEventClazz;
import com.dxfeed.api.events.DxfgOrderBase;
import org.graalvm.nativeimage.UnmanagedMemory;
import org.graalvm.nativeimage.c.struct.SizeOf;
import org.graalvm.nativeimage.c.type.CCharPointer;

public class OrderBaseMapper extends OrderAbstractMapper<OrderBase, DxfgOrderBase> {

  public OrderBaseMapper(final Mapper<String, CCharPointer> stringMapperForMarketEvent) {
    super(stringMapperForMarketEvent);
  }

  @Override
  public DxfgOrderBase createNativeObject() {
    final DxfgOrderBase nObject = UnmanagedMemory.calloc(SizeOf.get(DxfgOrderBase.class));
    nObject.setClazz(DxfgEventClazz.DXFG_EVENT_ORDER_BASE.getCValue());
    return nObject;
  }

  @Override
  public void fillNativeObject(final OrderBase jObject, final DxfgOrderBase nObject) {
    super.fillNativeObject(jObject, nObject);
  }

  @Override
  protected void cleanNativeObject(final DxfgOrderBase nObject) {
    super.cleanNativeObject(nObject);
  }

  @Override
  public OrderBase toJavaObject(final DxfgOrderBase nObject) {
    final OrderBase jObject = new OrderBase();
    fillJavaObject(nObject, jObject);
    return jObject;
  }

  @Override
  public void fillJavaObject(final DxfgOrderBase nObject, final OrderBase jObject) {
    super.fillJavaObject(nObject, jObject);
  }
}
