package com.dxfeed.event.market;

import com.dxfeed.api.maper.Mapper;
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
  public void fillNative(final SpreadOrder jObject, final DxfgSpreadOrder nObject) {
    super.fillNative(jObject, nObject);
    nObject.setSpreadSymbol(this.stringMapper.toNative(jObject.getSpreadSymbol()));
  }

  @Override
  public void cleanNative(final DxfgSpreadOrder nObject) {
    super.cleanNative(nObject);
    this.stringMapper.release(nObject.getSpreadSymbol());
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
    jObject.setSpreadSymbol(this.stringMapper.toJava(nObject.getSpreadSymbol()));
  }
}
