package com.dxfeed.event.market;

import com.dxfeed.api.events.DxfgEventKind;
import com.dxfeed.api.events.DxfgOrder;
import org.graalvm.nativeimage.c.struct.SizeOf;
import org.graalvm.nativeimage.c.type.CCharPointer;

public class OrderMapper extends OrderAbstractMapper<Order, DxfgOrder> {

  private final Mapper<String, CCharPointer> stringMapper;

  public OrderMapper(
      final MarketEventMapper marketEventMapper,
      final Mapper<String, CCharPointer> stringMapper
  ) {
    super(marketEventMapper);
    this.stringMapper = stringMapper;
  }

  @Override
  protected int size() {
    return SizeOf.get(DxfgOrder.class);
  }

  @Override
  protected void fillNativeObject(final DxfgOrder nObject, final Order jObject) {
    nObject.setKind(DxfgEventKind.DXFG_EVENT_TYPE_ORDER.getCValue());
    super.fillNativeObject(nObject, jObject);
    nObject.setMarketMaker(this.stringMapper.nativeObject(jObject.getMarketMaker()));
  }

  @Override
  protected void cleanNativeObject(final DxfgOrder nObject) {
    super.cleanNativeObject(nObject);
  }
}
