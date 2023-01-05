package com.dxfeed.api.events;

import com.dxfeed.api.javac.CPointerOnPointer;
import org.graalvm.nativeimage.c.CContext;
import org.graalvm.nativeimage.c.struct.CPointerTo;

@CContext(Directives.class)
@CPointerTo(DxfgOrder.class)
public interface DxfgOrderPointer extends CPointerOnPointer<DxfgOrder> {

}
