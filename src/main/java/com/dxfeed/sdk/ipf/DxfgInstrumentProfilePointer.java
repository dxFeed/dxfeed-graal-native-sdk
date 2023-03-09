package com.dxfeed.sdk.ipf;

import com.dxfeed.sdk.javac.CPointerOnPointer;
import org.graalvm.nativeimage.c.CContext;
import org.graalvm.nativeimage.c.struct.CPointerTo;
import org.graalvm.nativeimage.c.type.CCharPointer;

@CContext(Directives.class)
@CPointerTo(CCharPointer.class)
public interface DxfgInstrumentProfilePointer extends CPointerOnPointer<DxfgInstrumentProfile> {

}
