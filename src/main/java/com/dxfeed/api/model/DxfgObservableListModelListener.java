package com.dxfeed.api.model;

import com.dxfeed.api.javac.JavaObjectHandler;
import com.dxfeed.event.IndexedEvent;
import com.dxfeed.model.ObservableListModelListener;
import org.graalvm.nativeimage.c.CContext;
import org.graalvm.nativeimage.c.struct.CStruct;

@CContext(Directives.class)
@CStruct("dxfg_observable_list_model_listener_t")
public interface DxfgObservableListModelListener
    extends JavaObjectHandler<ObservableListModelListener<? super IndexedEvent<?>>> {

}
