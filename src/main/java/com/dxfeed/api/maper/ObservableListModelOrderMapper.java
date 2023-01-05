package com.dxfeed.api.maper;

import com.dxfeed.api.model.DxfgObservableListModelOrder;
import com.dxfeed.event.market.Order;
import com.dxfeed.model.ObservableListModel;
import org.graalvm.nativeimage.c.struct.SizeOf;

public class ObservableListModelOrderMapper
    extends JavaObjectHandlerMapper<ObservableListModel<Order>, DxfgObservableListModelOrder> {

  @Override
  protected int getSizeJavaObjectHandler() {
    return SizeOf.get(DxfgObservableListModelOrder.class);
  }
}
