package com.dxfeed.api.ipf;

import com.dxfeed.api.javac.CList;
import org.graalvm.nativeimage.c.CContext;
import org.graalvm.nativeimage.c.struct.CStruct;

@CContext(Directives.class)
@CStruct("dxfg_instrument_profile_list")
public interface DxfgInstrumentProfileList extends CList<DxfgInstrumentProfilePointer> {

}
