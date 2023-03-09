package com.dxfeed.sdk.ipf;

import com.dxfeed.ipf.InstrumentProfile;
import com.dxfeed.sdk.javac.JavaObjectHandler;
import java.util.Iterator;
import org.graalvm.nativeimage.c.CContext;
import org.graalvm.nativeimage.c.struct.CStruct;

@CContext(Directives.class)
@CStruct("dxfg_iterable_ip_t")
public interface DxfgIterableInstrumentProfile extends JavaObjectHandler<Iterator<InstrumentProfile>> {

}
