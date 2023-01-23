package com.dxfeed.api.ipf;

import com.dxfeed.api.javac.JavaObjectHandler;
import com.dxfeed.ipf.live.InstrumentProfileCollector;
import org.graalvm.nativeimage.c.CContext;
import org.graalvm.nativeimage.c.struct.CStruct;

@CContext(Directives.class)
@CStruct("dxfg_ipf_collector_t")
public interface DxfgInstrumentProfileCollector extends JavaObjectHandler<InstrumentProfileCollector> {

}
