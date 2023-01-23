package com.dxfeed.api.ipf;

import com.dxfeed.api.javac.JavaObjectHandler;
import com.dxfeed.ipf.live.InstrumentProfileConnection;
import org.graalvm.nativeimage.c.CContext;
import org.graalvm.nativeimage.c.struct.CStruct;

@CContext(Directives.class)
@CStruct("dxfg_ipf_connection_t")
public interface DxfgInstrumentProfileConnection extends JavaObjectHandler<InstrumentProfileConnection> {

}
