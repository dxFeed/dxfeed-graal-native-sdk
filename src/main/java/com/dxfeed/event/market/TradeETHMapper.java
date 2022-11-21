package com.dxfeed.event.market;

import com.dxfeed.api.events.DxfgEventClazz;
import com.dxfeed.api.events.DxfgTradeETH;
import org.graalvm.nativeimage.UnmanagedMemory;
import org.graalvm.nativeimage.c.struct.SizeOf;
import org.graalvm.nativeimage.c.type.CCharPointer;

public class TradeETHMapper extends TradeBaseMapper<TradeETH, DxfgTradeETH> {

  public TradeETHMapper(final Mapper<String, CCharPointer> stringMapperForMarketEvent) {
    super(stringMapperForMarketEvent);
  }

  @Override
  public DxfgTradeETH createNativeObject() {
    final DxfgTradeETH nObject = UnmanagedMemory.calloc(SizeOf.get(DxfgTradeETH.class));
    nObject.setClazz(DxfgEventClazz.DXFG_EVENT_TRADE_ETH.getCValue());
    return nObject;
  }

  @Override
  public void fillNativeObject(final TradeETH jObject, final DxfgTradeETH nObject) {
    super.fillNativeObject(jObject, nObject);
  }

  @Override
  protected void cleanNativeObject(final DxfgTradeETH nObject) {
    super.cleanNativeObject(nObject);
  }

  @Override
  public TradeETH toJavaObject(final DxfgTradeETH nObject) {
    final TradeETH jObject = new TradeETH();
    fillJavaObject(nObject, jObject);
    return jObject;
  }

  @Override
  public void fillJavaObject(final DxfgTradeETH nObject, final TradeETH jObject) {
    super.fillJavaObject(nObject, jObject);
  }
}
