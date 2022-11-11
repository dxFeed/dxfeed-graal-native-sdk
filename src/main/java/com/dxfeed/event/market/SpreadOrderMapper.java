package com.dxfeed.event.market;

import com.dxfeed.api.events.DxfgEventKind;
import com.dxfeed.api.events.DxfgSpreadOrder;
import org.graalvm.nativeimage.c.struct.SizeOf;
import org.graalvm.nativeimage.c.type.CCharPointer;

public class SpreadOrderMapper extends OrderAbstractMapper<SpreadOrder, DxfgSpreadOrder> {

  private final Mapper<String, CCharPointer> stringMapper;

  public SpreadOrderMapper(
      final MarketEventMapper marketEventMapper,
      final Mapper<String, CCharPointer> stringMapper
  ) {
    super(marketEventMapper);
    this.stringMapper = stringMapper;
  }

  @Override
  protected int size() {
    return SizeOf.get(DxfgSpreadOrder.class);
  }

  @Override
  protected void fillNativeObject(final DxfgSpreadOrder nObject, final SpreadOrder jObject) {
    nObject.setKind(DxfgEventKind.DXFG_EVENT_TYPE_SPREAD_ORDER.getCValue());
    super.fillNativeObject(nObject, jObject);
    nObject.setSpreadSymbol(this.stringMapper.nativeObject(jObject.getSpreadSymbol()));
  }

  @Override
  protected void cleanNativeObject(final DxfgSpreadOrder nObject) {
    super.cleanNativeObject(nObject);
    this.stringMapper.delete(nObject.getSpreadSymbol());
  }
}
