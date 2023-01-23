package com.dxfeed.api.javac;

import org.graalvm.nativeimage.c.CContext;
import org.graalvm.nativeimage.c.struct.CStruct;

@CContext(Directives.class)
@CStruct("dxfg_string_list")
public interface DxfgCharPointerList extends CList<DxfgCharPointerPointer> {

}
