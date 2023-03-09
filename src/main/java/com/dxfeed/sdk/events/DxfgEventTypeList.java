package com.dxfeed.sdk.events;

import com.dxfeed.sdk.javac.CList;
import org.graalvm.nativeimage.c.CContext;
import org.graalvm.nativeimage.c.struct.CStruct;

@CContext(Directives.class)
@CStruct("dxfg_event_type_list")
public interface DxfgEventTypeList extends CList<DxfgEventTypePointer> {

}
