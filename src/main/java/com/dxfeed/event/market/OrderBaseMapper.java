package com.dxfeed.event.market;

import com.dxfeed.api.events.DxfgEventKind;
import com.dxfeed.api.events.DxfgOrderBase;
import org.graalvm.nativeimage.c.struct.SizeOf;

public class OrderBaseMapper extends OrderAbstractMapper<OrderBase, DxfgOrderBase> {

  public OrderBaseMapper(final MarketEventMapper marketEventMapper) {
    super(marketEventMapper);
  }

  @Override
  protected int size() {
    return SizeOf.get(DxfgOrderBase.class);
  }

  @Override
  protected void fillNativeObject(final DxfgOrderBase nObject, final OrderBase jObject) {
    super.fillNativeObject(nObject, jObject);
    nObject.setKind(DxfgEventKind.DXFG_EVENT_TYPE_ORDER_BASE.getCValue());
  }

  @Override
  protected void cleanNativeObject(final DxfgOrderBase nObject) {
    super.cleanNativeObject(nObject);
  }
}
