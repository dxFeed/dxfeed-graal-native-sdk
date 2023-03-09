package com.dxfeed.sdk.feed;

import com.dxfeed.sdk.javac.CPointerOnPointer;
import org.graalvm.nativeimage.c.CContext;
import org.graalvm.nativeimage.c.struct.CPointerTo;

@CContext(Directives.class)
@CPointerTo(DxfgPromise.class)
public interface DxfgPromisePointer extends CPointerOnPointer<DxfgPromise> {

}
