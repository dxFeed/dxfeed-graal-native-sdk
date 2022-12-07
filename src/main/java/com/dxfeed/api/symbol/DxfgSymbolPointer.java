package com.dxfeed.api.symbol;

import com.dxfeed.api.javac.CPointerOnPointer;
import org.graalvm.nativeimage.c.CContext;
import org.graalvm.nativeimage.c.struct.CPointerTo;

@CContext(Directives.class)
@CPointerTo(DxfgSymbol.class)
public interface DxfgSymbolPointer extends CPointerOnPointer<DxfgSymbol> {

}
