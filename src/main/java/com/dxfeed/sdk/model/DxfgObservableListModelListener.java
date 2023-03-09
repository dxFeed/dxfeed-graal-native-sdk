package com.dxfeed.sdk.model;

import com.dxfeed.event.IndexedEvent;
import com.dxfeed.model.ObservableListModelListener;
import com.dxfeed.sdk.javac.JavaObjectHandler;
import org.graalvm.nativeimage.c.CContext;
import org.graalvm.nativeimage.c.struct.CStruct;

@CContext(Directives.class)
@CStruct("dxfg_observable_list_model_listener_t")
public interface DxfgObservableListModelListener
    extends JavaObjectHandler<ObservableListModelListener<? super IndexedEvent<?>>>
{

}
