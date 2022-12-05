package com.dxfeed.api.endpoint;

import com.dxfeed.api.javac.JavaObjectHandler;
import java.beans.PropertyChangeListener;
import org.graalvm.nativeimage.c.CContext;
import org.graalvm.nativeimage.c.struct.CStruct;

@CContext(Directives.class)
@CStruct("dxfg_endpoint_state_change_listener_t")
public interface DxfgEndpointStateChangeListener extends JavaObjectHandler<PropertyChangeListener> {

}
