package com.dxfeed.sdk.schedule;

import com.dxfeed.sdk.javac.CPointerOnPointer;
import org.graalvm.nativeimage.c.CContext;
import org.graalvm.nativeimage.c.struct.CPointerTo;

@CContext(Directives.class)
@CPointerTo(DxfgSession.class)
public interface DxfgSessionPointer extends CPointerOnPointer<DxfgSession> {

}
