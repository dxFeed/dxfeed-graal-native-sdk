package com.dxfeed.sdk.ipf;

import com.dxfeed.sdk.javac.CPointerPointer;
import org.graalvm.nativeimage.c.CContext;
import org.graalvm.nativeimage.c.struct.CPointerTo;

@CContext(Directives.class)
@CPointerTo(DxfgInstrumentProfileCustomFieldsHandle.class)
public interface DxfgInstrumentProfileCustomFieldsHandlePointer extends
    CPointerPointer<DxfgInstrumentProfileCustomFieldsHandle> {

}
