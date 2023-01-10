package com.dxfeed.api.model;

import com.dxfeed.api.javac.JavaObjectHandler;
import com.dxfeed.model.IndexedEventModel;
import org.graalvm.nativeimage.c.CContext;
import org.graalvm.nativeimage.c.struct.CStruct;

@CContext(Directives.class)
@CStruct("dxfg_indexed_event_model_t")
public interface DxfgIndexedEventModel extends JavaObjectHandler<IndexedEventModel<?>> {

}
