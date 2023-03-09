package com.dxfeed.sdk.ipf;

import com.dxfeed.sdk.javac.JavaObjectHandler;
import java.beans.PropertyChangeListener;
import org.graalvm.nativeimage.c.CContext;
import org.graalvm.nativeimage.c.struct.CStruct;

@CContext(Directives.class)
@CStruct("dxfg_ipf_connection_state_change_listener_t")
public interface DxfgIpfConnectionStateChangeListener extends JavaObjectHandler<PropertyChangeListener> {

}

