package com.dxfeed.api.feed;

import com.dxfeed.api.javac.CPointerOnPointer;
import org.graalvm.nativeimage.c.CContext;
import org.graalvm.nativeimage.c.struct.CPointerTo;

@CContext(Directives.class)
@CPointerTo(DxfgPromise.class)
public interface DxfgPromisePointer extends CPointerOnPointer<DxfgPromise> {

}
