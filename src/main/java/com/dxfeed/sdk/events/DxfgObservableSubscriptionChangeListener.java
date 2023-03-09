package com.dxfeed.sdk.events;

import com.dxfeed.api.osub.ObservableSubscriptionChangeListener;
import com.dxfeed.sdk.javac.JavaObjectHandler;
import org.graalvm.nativeimage.c.CContext;
import org.graalvm.nativeimage.c.struct.CStruct;

@CContext(Directives.class)
@CStruct("dxfg_observable_subscription_change_listener_t")
public interface DxfgObservableSubscriptionChangeListener
    extends JavaObjectHandler<ObservableSubscriptionChangeListener>
{

}
