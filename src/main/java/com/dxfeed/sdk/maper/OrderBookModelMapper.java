package com.dxfeed.sdk.maper;

import com.dxfeed.model.market.OrderBookModel;
import com.dxfeed.sdk.model.DxfgOrderBookModel;
import org.graalvm.nativeimage.c.struct.SizeOf;

public class OrderBookModelMapper
    extends JavaObjectHandlerMapper<OrderBookModel, DxfgOrderBookModel> {

  @Override
  protected int getSizeJavaObjectHandler() {
    return SizeOf.get(DxfgOrderBookModel.class);
  }
}
