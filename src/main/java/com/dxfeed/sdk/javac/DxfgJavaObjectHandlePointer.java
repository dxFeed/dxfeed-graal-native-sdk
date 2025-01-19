package com.dxfeed.sdk.javac;

import org.graalvm.nativeimage.c.CContext;
import org.graalvm.nativeimage.c.struct.CPointerTo;

@CContext(Directives.class)
@CPointerTo(DxfgJavaObjectHandle.class)
public interface DxfgJavaObjectHandlePointer extends CPointerPointer<DxfgJavaObjectHandle> {

}
