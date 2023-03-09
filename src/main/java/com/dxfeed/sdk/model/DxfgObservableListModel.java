package com.dxfeed.sdk.model;

import com.dxfeed.event.IndexedEvent;
import com.dxfeed.model.ObservableListModel;
import com.dxfeed.sdk.javac.JavaObjectHandler;
import org.graalvm.nativeimage.c.CContext;
import org.graalvm.nativeimage.c.struct.CStruct;

@CContext(Directives.class)
@CStruct("dxfg_observable_list_model_t")
public interface DxfgObservableListModel
    extends JavaObjectHandler<ObservableListModel<? extends IndexedEvent<?>>>
{

}
