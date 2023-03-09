package com.dxfeed.sdk.model;

import com.dxfeed.model.market.OrderBookModel;
import com.dxfeed.sdk.javac.JavaObjectHandler;
import org.graalvm.nativeimage.c.CContext;
import org.graalvm.nativeimage.c.struct.CStruct;

@CContext(Directives.class)
@CStruct("dxfg_order_book_model_t")
public interface DxfgOrderBookModel extends JavaObjectHandler<OrderBookModel> {

}
