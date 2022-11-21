package com.dxfeed.api.javac;

import org.graalvm.nativeimage.c.CContext;
import org.graalvm.nativeimage.c.struct.CStruct;

@CContext(Directives.class)
@CStruct("dxfg_java_object_handler_list")
public interface DxfgJavaObjectHandlerList extends CList<DxfgJavaObjectHandlerPointer> {

}
