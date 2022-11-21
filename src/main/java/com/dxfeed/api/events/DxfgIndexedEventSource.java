package com.dxfeed.api.events;

import com.dxfeed.api.javac.JavaObjectHandler;
import com.dxfeed.event.IndexedEventSource;
import org.graalvm.nativeimage.c.CContext;
import org.graalvm.nativeimage.c.struct.CStruct;

@CContext(Directives.class)
@CStruct("dxfg_indexed_event_source")
public interface DxfgIndexedEventSource extends JavaObjectHandler<IndexedEventSource> {

}
