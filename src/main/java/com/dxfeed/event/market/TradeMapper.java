package com.dxfeed.event.market;

import com.dxfeed.api.maper.Mapper;
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
  public void fillNative(final Trade jObject, final DxfgTrade nObject) {
    super.fillNative(jObject, nObject);
  }

  @Override
  public void cleanNative(final DxfgTrade nObject) {
    super.cleanNative(nObject);
  }

  @Override
  protected Trade doToJava(final DxfgTrade nObject) {
    final Trade jObject = new Trade();
    this.fillJava(nObject, jObject);
    return jObject;
  }

  @Override
  public void fillJava(final DxfgTrade nObject, final Trade jObject) {
    super.fillJava(nObject, jObject);
  }
}
