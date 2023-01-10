package com.dxfeed.api.maper;

import com.dxfeed.api.model.DxfgObservableListModelListener;
import com.dxfeed.event.IndexedEvent;
import com.dxfeed.model.ObservableListModelListener;
import org.graalvm.nativeimage.c.struct.SizeOf;

public class ObservableListModelListenerMapper
    extends JavaObjectHandlerMapper<ObservableListModelListener<? super IndexedEvent<?>>, DxfgObservableListModelListener> {

  @Override
  protected int getSizeJavaObjectHandler() {
    return SizeOf.get(DxfgObservableListModelListener.class);
  }
}
