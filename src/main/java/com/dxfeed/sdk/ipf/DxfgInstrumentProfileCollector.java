package com.dxfeed.sdk.ipf;

import com.dxfeed.ipf.live.InstrumentProfileCollector;
import com.dxfeed.sdk.javac.JavaObjectHandler;
import org.graalvm.nativeimage.c.CContext;
import org.graalvm.nativeimage.c.struct.CStruct;

@CContext(Directives.class)
@CStruct("dxfg_ipf_collector_t")
public interface DxfgInstrumentProfileCollector extends JavaObjectHandler<InstrumentProfileCollector> {

}
