package com.dxfeed.sdk.exception;

import com.dxfeed.sdk.javac.CList;
import org.graalvm.nativeimage.c.CContext;
import org.graalvm.nativeimage.c.struct.CStruct;

@CContext(Directives.class)
@CStruct("dxfg_stack_trace_element_list")
public interface DxfgStackTraceElementList extends CList<DxfgStackTraceElementPointer> {

}
