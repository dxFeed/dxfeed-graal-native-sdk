package com.dxfeed.api.events;

import com.dxfeed.api.javac.CPointerOnPointer;
import org.graalvm.nativeimage.c.CContext;
import org.graalvm.nativeimage.c.struct.CPointerTo;

@CContext(Directives.class)
@CPointerTo(DxfgEventType.class)
public interface DxfgEventTypePointer extends CPointerOnPointer<DxfgEventType> {

}
