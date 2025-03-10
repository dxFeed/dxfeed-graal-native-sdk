package com.dxfeed.event.market;

import com.dxfeed.sdk.events.DxfgEventClazz;
import com.dxfeed.sdk.events.DxfgSpreadOrder;
import com.dxfeed.sdk.mappers.Mapper;
import org.graalvm.nativeimage.UnmanagedMemory;
import org.graalvm.nativeimage.c.struct.SizeOf;
import org.graalvm.nativeimage.c.type.CCharPointer;

public class SpreadOrderMapper extends OrderAbstractMapper<SpreadOrder, DxfgSpreadOrder> {

  public SpreadOrderMapper(
      final Mapper<String, CCharPointer> stringMapper
  ) {
    super(stringMapper);
  }

  @Override
  public DxfgSpreadOrder createNativeObject() {
    final DxfgSpreadOrder nObject = UnmanagedMemory.calloc(SizeOf.get(DxfgSpreadOrder.class));
    nObject.setClazz(DxfgEventClazz.DXFG_EVENT_SPREAD_ORDER.getCValue());
    return nObject;
  }

  @Override
  public void fillNative(final SpreadOrder jObject, final DxfgSpreadOrder nObject, boolean clean) {
    super.fillNative(jObject, nObject, clean);
    nObject.setSpreadSymbol(stringMapper.toNative(jObject.getSpreadSymbol()));
  }

  @Override
  public void cleanNative(final DxfgSpreadOrder nObject) {
    super.cleanNative(nObject);
    stringMapper.release(nObject.getSpreadSymbol());
  }

  @Override
  protected SpreadOrder doToJava(final DxfgSpreadOrder nObject) {
    final SpreadOrder jObject = new SpreadOrder();
    this.fillJava(nObject, jObject);
    return jObject;
  }

  @Override
  public void fillJava(final DxfgSpreadOrder nObject, final SpreadOrder jObject) {
    super.fillJava(nObject, jObject);
    jObject.setSpreadSymbol(stringMapper.toJava(nObject.getSpreadSymbol()));
  }
}
