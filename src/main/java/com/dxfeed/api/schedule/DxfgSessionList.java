package com.dxfeed.api.schedule;

import com.dxfeed.api.javac.CList;
import org.graalvm.nativeimage.c.CContext;
import org.graalvm.nativeimage.c.struct.CStruct;

@CContext(Directives.class)
@CStruct("dxfg_session_list")
public interface DxfgSessionList extends CList<DxfgSessionPointer> {

}
