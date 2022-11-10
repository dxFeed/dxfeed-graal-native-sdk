package com.dxfeed.event.market;

import com.dxfeed.api.events.DxfgEventKind;
import com.dxfeed.api.events.DxfgOrderBase;
import org.graalvm.nativeimage.c.struct.SizeOf;

public class OrderBaseMapper extends OrderAbstractMapper<OrderBase, DxfgOrderBase> {

  public OrderBaseMapper(final StringMapper stringMapper) {
    super(stringMapper);
  }

  @Override
  protected int size() {
    return SizeOf.get(DxfgOrderBase.class);
  }

  @Override
  protected void doFillNativeObject(final DxfgOrderBase nObject, final OrderBase jObject) {
    nObject.setKind(DxfgEventKind.DXFG_EVENT_TYPE_ORDER_BASE.getCValue());
    super.doFillNativeObject(nObject, jObject);
  }

  @Override
  protected void doCleanNativeObject(final DxfgOrderBase nObject) {
    // nothing
  }
}
