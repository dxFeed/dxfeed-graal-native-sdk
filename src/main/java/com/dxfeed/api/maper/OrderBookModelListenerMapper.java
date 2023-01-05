package com.dxfeed.api.maper;

import com.dxfeed.api.model.DxfgOrderBookModelListener;
import com.dxfeed.model.market.OrderBookModelListener;
import org.graalvm.nativeimage.c.struct.SizeOf;

public class OrderBookModelListenerMapper
    extends JavaObjectHandlerMapper<OrderBookModelListener, DxfgOrderBookModelListener> {

  @Override
  protected int getSizeJavaObjectHandler() {
    return SizeOf.get(DxfgOrderBookModelListener.class);
  }
}
