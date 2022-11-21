package com.dxfeed.api.javac;

import org.graalvm.nativeimage.c.CContext;
import org.graalvm.nativeimage.c.struct.CPointerTo;

@CContext(Directives.class)
@CPointerTo(JavaObjectHandler.class)
public interface DxfgJavaObjectHandlerPointer extends CPointerOnPointer<JavaObjectHandler<?>> {

}
