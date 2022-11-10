package com.dxfeed.event.market;

import com.dxfeed.api.events.DxfgEventKind;
import com.dxfeed.api.events.DxfgOrder;
import org.graalvm.nativeimage.c.struct.SizeOf;

public class OrderMapper extends OrderAbstractMapper<Order, DxfgOrder> {

  public OrderMapper(final StringMapper stringMapper) {
    super(stringMapper);
  }

  @Override
  protected int size() {
    return SizeOf.get(DxfgOrder.class);
  }

  @Override
  protected void doFillNativeObject(final DxfgOrder nObject, final Order jObject) {
    nObject.setKind(DxfgEventKind.DXFG_EVENT_TYPE_ORDER.getCValue());
    super.doFillNativeObject(nObject, jObject);
    nObject.setMarketMaker(super.stringMapper.nativeObject(jObject.getMarketMaker()));
  }

  @Override
  protected void doCleanNativeObject(final DxfgOrder nObject) {
    super.stringMapper.delete(nObject.getMarketMaker());
  }
}
