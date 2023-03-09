package com.dxfeed.sdk.events;

import com.dxfeed.sdk.javac.CPointerOnPointer;
import org.graalvm.nativeimage.c.CContext;
import org.graalvm.nativeimage.c.struct.CPointerTo;
import org.graalvm.nativeimage.c.type.CIntPointer;

@CContext(Directives.class)
@CPointerTo(CIntPointer.class)
public interface DxfgEventClazzPointer extends CPointerOnPointer<CIntPointer> {

}
