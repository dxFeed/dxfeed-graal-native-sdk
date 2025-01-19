package com.dxfeed.sdk.schedule;

import com.dxfeed.sdk.javac.CPointerPointer;
import org.graalvm.nativeimage.c.CContext;
import org.graalvm.nativeimage.c.struct.CPointerTo;

@CContext(Directives.class)
@CPointerTo(DxfgScheduleHandle.class)
public interface DxfgScheduleHandlePointer extends CPointerPointer<DxfgScheduleHandle> {

}
