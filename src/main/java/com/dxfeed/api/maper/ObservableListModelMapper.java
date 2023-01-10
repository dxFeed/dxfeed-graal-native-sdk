package com.dxfeed.api.maper;

import com.dxfeed.api.model.DxfgObservableListModel;
import com.dxfeed.event.IndexedEvent;
import com.dxfeed.model.ObservableListModel;
import org.graalvm.nativeimage.c.struct.SizeOf;

public class ObservableListModelMapper
    extends JavaObjectHandlerMapper<ObservableListModel<? extends IndexedEvent<?>>, DxfgObservableListModel> {

  @Override
  protected int getSizeJavaObjectHandler() {
    return SizeOf.get(DxfgObservableListModel.class);
  }
}
