package com.dxfeed.api.subscription;

import com.dxfeed.api.DXFeedEventListener;
import com.dxfeed.api.javac.JavaObjectHandler;
import com.dxfeed.event.EventType;
import org.graalvm.nativeimage.c.CContext;
import org.graalvm.nativeimage.c.struct.CStruct;

@CContext(Directives.class)
@CStruct("dxfg_feed_event_listener_t")
public interface DxfgFeedEventListener
    extends JavaObjectHandler<DXFeedEventListener<EventType<?>>> {

}
