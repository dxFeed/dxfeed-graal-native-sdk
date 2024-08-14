package com.dxfeed.sdk.events;

import com.dxfeed.sdk.javac.CPointerOnPointer;
import com.dxfeed.sdk.source.DxfgIndexedEventSource;
import org.graalvm.nativeimage.c.CContext;
import org.graalvm.nativeimage.c.struct.CPointerTo;

@CContext(Directives.class)
@CPointerTo(DxfgIndexedEventSource.class)
public interface DxfgIndexedEventSourcePointer extends CPointerOnPointer<DxfgIndexedEventSource> {

}
