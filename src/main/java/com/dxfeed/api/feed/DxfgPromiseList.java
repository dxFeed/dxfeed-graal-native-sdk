package com.dxfeed.api.feed;

import com.dxfeed.api.javac.CList;
import org.graalvm.nativeimage.c.CContext;
import org.graalvm.nativeimage.c.struct.CStruct;

@CContext(Directives.class)
@CStruct("dxfg_promise_list")
public interface DxfgPromiseList extends CList<DxfgPromisePointer> {

}
