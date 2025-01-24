package com.dxfeed.sdk.javac;

import org.graalvm.nativeimage.c.CContext;
import org.graalvm.nativeimage.c.struct.CPointerTo;

@CContext(Directives.class)
@CPointerTo(DxfgCStringListPointer.class)
public interface DxfgCStringListPointerPointer extends CPointerPointer<DxfgCStringListPointer> {

}
