package com.dxfeed.event.market;

import com.dxfeed.api.events.DxfgEventKind;
import com.dxfeed.api.events.DxfgSpreadOrder;
import org.graalvm.nativeimage.c.struct.SizeOf;

public class SpreadOrderMapper extends OrderAbstractMapper<SpreadOrder, DxfgSpreadOrder> {

  public SpreadOrderMapper(final StringMapper stringMapper) {
    super(stringMapper);
  }

  @Override
  protected int size() {
    return SizeOf.get(DxfgSpreadOrder.class);
  }

  @Override
  protected void doFillNativeObject(final DxfgSpreadOrder nObject, final SpreadOrder jObject) {
    nObject.setKind(DxfgEventKind.DXFG_EVENT_TYPE_SPREAD_ORDER.getCValue());
    super.doFillNativeObject(nObject, jObject);
    nObject.setSpreadSymbol(super.stringMapper.nativeObject(jObject.getSpreadSymbol()));
  }

  @Override
  protected void doCleanNativeObject(final DxfgSpreadOrder nObject) {
    super.stringMapper.delete(nObject.getSpreadSymbol());
  }
}
