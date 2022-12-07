package com.dxfeed.api.symbol;

import org.graalvm.nativeimage.c.CContext;
import org.graalvm.nativeimage.c.struct.CStruct;

@CContext(Directives.class)
@CStruct("dxfg_wildcard_symbol_t")
public interface DxfgWildcardSymbol extends DxfgSymbol {

}
