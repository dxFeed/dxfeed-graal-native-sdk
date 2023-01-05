package com.dxfeed.api.model;

import com.dxfeed.api.javac.JavaObjectHandler;
import com.dxfeed.event.market.Order;
import com.dxfeed.model.ObservableListModel;
import org.graalvm.nativeimage.c.CContext;
import org.graalvm.nativeimage.c.struct.CStruct;

@CContext(Directives.class)
@CStruct("dxfg_observable_list_model_order_t")
public interface DxfgObservableListModelOrder
    extends JavaObjectHandler<ObservableListModel<Order>> {

}
