package com.dxfeed.api.events;

import com.dxfeed.api.javac.CPointerOnPointer;
import org.graalvm.nativeimage.c.CContext;
import org.graalvm.nativeimage.c.struct.CPointerTo;
import org.graalvm.nativeimage.c.type.CIntPointer;

@CContext(Directives.class)
@CPointerTo(CIntPointer.class)
public interface DxfgEventClazzPointer extends CPointerOnPointer<CIntPointer> {

}
