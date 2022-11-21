package com.dxfeed.event.market;

import com.dxfeed.api.events.DxfgEventClazz;
import com.dxfeed.api.events.DxfgSpreadOrder;
import org.graalvm.nativeimage.UnmanagedMemory;
import org.graalvm.nativeimage.c.struct.SizeOf;
import org.graalvm.nativeimage.c.type.CCharPointer;

public class SpreadOrderMapper extends OrderAbstractMapper<SpreadOrder, DxfgSpreadOrder> {

  private final Mapper<String, CCharPointer> stringMapper;

  public SpreadOrderMapper(
      final Mapper<String, CCharPointer> stringMapperForMarketEvent,
      final Mapper<String, CCharPointer> stringMapper
  ) {
    super(stringMapperForMarketEvent);
    this.stringMapper = stringMapper;
  }

  @Override
  public DxfgSpreadOrder createNativeObject() {
    final DxfgSpreadOrder nObject = UnmanagedMemory.calloc(SizeOf.get(DxfgSpreadOrder.class));
    nObject.setClazz(DxfgEventClazz.DXFG_EVENT_SPREAD_ORDER.getCValue());
    return nObject;
  }

  @Override
  public void fillNativeObject(final SpreadOrder jObject, final DxfgSpreadOrder nObject) {
    super.fillNativeObject(jObject, nObject);
    nObject.setSpreadSymbol(this.stringMapper.toNativeObject(jObject.getSpreadSymbol()));
  }

  @Override
  protected void cleanNativeObject(final DxfgSpreadOrder nObject) {
    super.cleanNativeObject(nObject);
    this.stringMapper.release(nObject.getSpreadSymbol());
  }

  @Override
  public SpreadOrder toJavaObject(final DxfgSpreadOrder nObject) {
    final SpreadOrder jObject = new SpreadOrder();
    fillJavaObject(nObject, jObject);
    return jObject;
  }

  @Override
  public void fillJavaObject(final DxfgSpreadOrder nObject, final SpreadOrder jObject) {
    super.fillJavaObject(nObject, jObject);
    jObject.setSpreadSymbol(this.stringMapper.toJavaObject(nObject.getSpreadSymbol()));
  }
}
