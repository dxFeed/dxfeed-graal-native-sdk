package com.dxfeed.event.market;

import com.dxfeed.api.Mapper;
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
  public void fillNative(final TradeETH jObject, final DxfgTradeETH nObject) {
    super.fillNative(jObject, nObject);
  }

  @Override
  public void cleanNative(final DxfgTradeETH nObject) {
    super.cleanNative(nObject);
  }

  @Override
  public TradeETH toJava(final DxfgTradeETH nObject) {
    final TradeETH jObject = new TradeETH();
    this.fillJava(nObject, jObject);
    return jObject;
  }

  @Override
  public void fillJava(final DxfgTradeETH nObject, final TradeETH jObject) {
    super.fillJava(nObject, jObject);
  }
}
