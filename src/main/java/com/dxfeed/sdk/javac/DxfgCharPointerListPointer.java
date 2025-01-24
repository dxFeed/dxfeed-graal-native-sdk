package com.dxfeed.sdk.javac;

import org.graalvm.nativeimage.c.CContext;
import org.graalvm.nativeimage.c.struct.CPointerTo;

@CContext(Directives.class)
@CPointerTo(DxfgCharPointerList.class)
public interface DxfgCharPointerListPointer extends CPointerPointer<DxfgCharPointerList> {

}
