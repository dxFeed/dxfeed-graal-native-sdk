package com.dxfeed.sdk.symbol;

import com.dxfeed.sdk.javac.CList;
import org.graalvm.nativeimage.c.CContext;
import org.graalvm.nativeimage.c.struct.CStruct;

@CContext(Directives.class)
@CStruct("dxfg_symbol_list")
public interface DxfgSymbolList extends CList<DxfgSymbolPointer> {

}
