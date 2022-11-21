package com.dxfeed.api.subscription;

import com.dxfeed.api.DXFeedSubscription;
import com.dxfeed.api.javac.JavaObjectHandler;
import com.dxfeed.event.EventType;
import org.graalvm.nativeimage.c.CContext;
import org.graalvm.nativeimage.c.struct.CStruct;

@CContext(Directives.class)
@CStruct("dxfg_subscription_t")
public interface DxfgSubscription<T extends DXFeedSubscription<? extends EventType<?>>>
    extends JavaObjectHandler<T> {

}