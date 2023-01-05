package com.dxfeed.api.maper;

import com.dxfeed.api.model.DxfgObservableListModelListener;
import com.dxfeed.event.market.Order;
import com.dxfeed.model.ObservableListModelListener;
import org.graalvm.nativeimage.c.struct.SizeOf;

public class ObservableListModelListenerMapper
    extends JavaObjectHandlerMapper<ObservableListModelListener<Order>, DxfgObservableListModelListener> {

  @Override
  protected int getSizeJavaObjectHandler() {
    return SizeOf.get(DxfgObservableListModelListener.class);
  }
}
