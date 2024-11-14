package com.dxfeed.sdk.mappers;

import com.dxfeed.model.market.OrderBookModelListener;
import com.dxfeed.sdk.model.DxfgOrderBookModelListener;
import org.graalvm.nativeimage.c.struct.SizeOf;

public class OrderBookModelListenerMapper
    extends JavaObjectHandlerMapper<OrderBookModelListener, DxfgOrderBookModelListener> {

  @Override
  protected int getSizeJavaObjectHandler() {
    return SizeOf.get(DxfgOrderBookModelListener.class);
  }
}
