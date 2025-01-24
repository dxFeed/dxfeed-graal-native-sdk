package com.dxfeed.sdk.javac;

import org.graalvm.nativeimage.c.CContext;
import org.graalvm.nativeimage.c.struct.CStruct;

@CContext(Directives.class)
@CStruct("dxfg_string_list")
public interface DxfgCStringListPointer extends CList<DxfgCharPointerPointer> {

}
