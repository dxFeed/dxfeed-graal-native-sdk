package com.dxfeed.event.market;

import com.dxfeed.sdk.events.DxfgEventClazz;
import com.dxfeed.sdk.events.DxfgOrderBase;
import com.dxfeed.sdk.maper.Mapper;
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
  public void fillNative(final OrderBase jObject, final DxfgOrderBase nObject) {
    super.fillNative(jObject, nObject);
  }

  @Override
  public void cleanNative(final DxfgOrderBase nObject) {
    super.cleanNative(nObject);
  }

  @Override
  protected OrderBase doToJava(final DxfgOrderBase nObject) {
    final OrderBase jObject = new OrderBase();
    this.fillJava(nObject, jObject);
    return jObject;
  }

  @Override
  public void fillJava(final DxfgOrderBase nObject, final OrderBase jObject) {
    super.fillJava(nObject, jObject);
  }
}
