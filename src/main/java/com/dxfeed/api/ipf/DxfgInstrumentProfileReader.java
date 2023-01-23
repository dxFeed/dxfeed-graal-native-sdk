package com.dxfeed.api.ipf;

import com.dxfeed.api.javac.JavaObjectHandler;
import com.dxfeed.ipf.InstrumentProfileReader;
import org.graalvm.nativeimage.c.CContext;
import org.graalvm.nativeimage.c.struct.CStruct;

@CContext(Directives.class)
@CStruct("dxfg_instrument_profile_reader_t")
public interface DxfgInstrumentProfileReader extends JavaObjectHandler<InstrumentProfileReader> {

}