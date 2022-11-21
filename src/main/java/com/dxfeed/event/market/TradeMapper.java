package com.dxfeed.event.market;

import com.dxfeed.api.events.DxfgEventClazz;
import com.dxfeed.api.events.DxfgTrade;
import org.graalvm.nativeimage.UnmanagedMemory;
import org.graalvm.nativeimage.c.struct.SizeOf;
import org.graalvm.nativeimage.c.type.CCharPointer;

public class TradeMapper extends TradeBaseMapper<Trade, DxfgTrade> {

  public TradeMapper(final Mapper<String, CCharPointer> stringMapperForMarketEvent) {
    super(stringMapperForMarketEvent);
  }

  @Override
  public DxfgTrade createNativeObject() {
    final DxfgTrade nObject = UnmanagedMemory.calloc(SizeOf.get(DxfgTrade.class));
    nObject.setClazz(DxfgEventClazz.DXFG_EVENT_TRADE.getCValue());
    return nObject;
  }

  @Override
  public void fillNativeObject(final Trade jObject, final DxfgTrade nObject) {
    super.fillNativeObject(jObject, nObject);
  }

  @Override
  protected void cleanNativeObject(final DxfgTrade nObject) {
    super.cleanNativeObject(nObject);
  }

  @Override
  public Trade toJavaObject(final DxfgTrade nObject) {
    final Trade jObject = new Trade();
    fillJavaObject(nObject, jObject);
    return jObject;
  }

  @Override
  public void fillJavaObject(final DxfgTrade nObject, final Trade jObject) {
    super.fillJavaObject(nObject, jObject);
  }
}
