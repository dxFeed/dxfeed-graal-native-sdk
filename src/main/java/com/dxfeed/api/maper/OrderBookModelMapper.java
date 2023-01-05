package com.dxfeed.api.maper;

import com.dxfeed.api.model.DxfgOrderBookModel;
import com.dxfeed.model.market.OrderBookModel;
import org.graalvm.nativeimage.c.struct.SizeOf;

public class OrderBookModelMapper
    extends JavaObjectHandlerMapper<OrderBookModel, DxfgOrderBookModel> {

  @Override
  protected int getSizeJavaObjectHandler() {
    return SizeOf.get(DxfgOrderBookModel.class);
  }
}
