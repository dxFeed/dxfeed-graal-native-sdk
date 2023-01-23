package com.dxfeed.api.ipf;

import com.dxfeed.api.javac.JavaObjectHandler;
import com.dxfeed.ipf.InstrumentProfile;
import java.util.Iterator;
import org.graalvm.nativeimage.c.CContext;
import org.graalvm.nativeimage.c.struct.CStruct;

@CContext(Directives.class)
@CStruct("dxfg_iterable_ip_t")
public interface DxfgIterableInstrumentProfile extends JavaObjectHandler<Iterator<InstrumentProfile>> {

}
