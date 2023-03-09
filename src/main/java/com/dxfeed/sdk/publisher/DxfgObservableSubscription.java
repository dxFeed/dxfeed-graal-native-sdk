package com.dxfeed.sdk.publisher;

import com.dxfeed.api.osub.ObservableSubscription;
import com.dxfeed.event.EventType;
import com.dxfeed.sdk.javac.JavaObjectHandler;
import org.graalvm.nativeimage.c.CContext;
import org.graalvm.nativeimage.c.struct.CStruct;

@CContext(Directives.class)
@CStruct("dxfg_observable_subscription_t")
public interface DxfgObservableSubscription
    extends JavaObjectHandler<ObservableSubscription<? extends EventType<?>>> {

}
