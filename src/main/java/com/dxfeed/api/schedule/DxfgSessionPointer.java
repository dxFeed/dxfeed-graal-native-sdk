package com.dxfeed.api.schedule;

import com.dxfeed.api.javac.CPointerOnPointer;
import org.graalvm.nativeimage.c.CContext;
import org.graalvm.nativeimage.c.struct.CPointerTo;

@CContext(Directives.class)
@CPointerTo(DxfgSession.class)
public interface DxfgSessionPointer extends CPointerOnPointer<DxfgSession> {

}
