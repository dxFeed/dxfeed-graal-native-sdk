package com.dxfeed.sdk.exception;

import com.dxfeed.sdk.javac.CPointerPointer;
import org.graalvm.nativeimage.c.CContext;
import org.graalvm.nativeimage.c.struct.CPointerTo;

@CContext(Directives.class)
@CPointerTo(DxfgStackTraceElement.class)
public interface DxfgStackTraceElementPointer extends CPointerPointer<DxfgStackTraceElement> {

}
