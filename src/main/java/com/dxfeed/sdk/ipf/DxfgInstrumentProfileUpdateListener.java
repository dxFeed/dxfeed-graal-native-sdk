package com.dxfeed.sdk.ipf;

import com.dxfeed.ipf.live.InstrumentProfileUpdateListener;
import com.dxfeed.sdk.javac.JavaObjectHandler;
import org.graalvm.nativeimage.c.CContext;
import org.graalvm.nativeimage.c.struct.CStruct;

@CContext(Directives.class)
@CStruct("dxfg_ipf_update_listener_t")
public interface DxfgInstrumentProfileUpdateListener extends JavaObjectHandler<InstrumentProfileUpdateListener> {

}
