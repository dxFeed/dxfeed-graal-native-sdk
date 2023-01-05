package com.dxfeed.api.events;

import com.dxfeed.api.javac.CList;
import org.graalvm.nativeimage.c.CContext;
import org.graalvm.nativeimage.c.struct.CStruct;

@CContext(Directives.class)
@CStruct("dxfg_order_list_t")
public interface DxfgOrderList extends CList<DxfgOrderPointer> {

}
