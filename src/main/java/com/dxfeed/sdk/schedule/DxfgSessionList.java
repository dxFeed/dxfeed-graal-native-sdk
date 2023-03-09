package com.dxfeed.sdk.schedule;

import com.dxfeed.sdk.javac.CList;
import org.graalvm.nativeimage.c.CContext;
import org.graalvm.nativeimage.c.struct.CStruct;

@CContext(Directives.class)
@CStruct("dxfg_session_list")
public interface DxfgSessionList extends CList<DxfgSessionPointer> {

}
