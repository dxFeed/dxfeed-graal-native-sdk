package com.dxfeed.sdk.events;

import com.dxfeed.sdk.javac.CList;
import org.graalvm.nativeimage.c.CContext;
import org.graalvm.nativeimage.c.struct.CStruct;

@CContext(Directives.class)
@CStruct("dxfg_indexed_event_source_list")
public interface DxfgIndexedEventSourceList extends CList<DxfgIndexedEventSourcePointer> {

}
