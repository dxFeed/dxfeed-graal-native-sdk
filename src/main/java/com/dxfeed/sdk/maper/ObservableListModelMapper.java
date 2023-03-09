package com.dxfeed.sdk.maper;

import com.dxfeed.event.IndexedEvent;
import com.dxfeed.model.ObservableListModel;
import com.dxfeed.sdk.model.DxfgObservableListModel;
import org.graalvm.nativeimage.c.struct.SizeOf;

public class ObservableListModelMapper
    extends JavaObjectHandlerMapper<ObservableListModel<? extends IndexedEvent<?>>, DxfgObservableListModel> {

  @Override
  protected int getSizeJavaObjectHandler() {
    return SizeOf.get(DxfgObservableListModel.class);
  }
}
