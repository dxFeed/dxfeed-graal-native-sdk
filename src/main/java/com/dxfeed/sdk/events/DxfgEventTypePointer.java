package com.dxfeed.sdk.events;

import com.dxfeed.sdk.javac.CPointerOnPointer;
import org.graalvm.nativeimage.c.CContext;
import org.graalvm.nativeimage.c.struct.CPointerTo;

@CContext(Directives.class)
@CPointerTo(DxfgEventType.class)
public interface DxfgEventTypePointer extends CPointerOnPointer<DxfgEventType> {

}
