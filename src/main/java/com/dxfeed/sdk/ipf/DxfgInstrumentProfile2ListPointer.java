package com.dxfeed.sdk.ipf;

import com.dxfeed.sdk.javac.CList;
import org.graalvm.nativeimage.c.CContext;
import org.graalvm.nativeimage.c.struct.CStruct;

@CContext(Directives.class)
@CStruct("dxfg_instrument_profile2_list_t")
public interface DxfgInstrumentProfile2ListPointer extends CList<DxfgInstrumentProfile2PointerPointer> {

}
