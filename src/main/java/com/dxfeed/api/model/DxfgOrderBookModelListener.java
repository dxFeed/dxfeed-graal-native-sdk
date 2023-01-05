package com.dxfeed.api.model;

import com.dxfeed.api.javac.JavaObjectHandler;
import com.dxfeed.model.market.OrderBookModelListener;
import org.graalvm.nativeimage.c.CContext;
import org.graalvm.nativeimage.c.struct.CStruct;

@CContext(Directives.class)
@CStruct("dxfg_order_book_model_listener_t")
public interface DxfgOrderBookModelListener extends JavaObjectHandler<OrderBookModelListener> {

}
