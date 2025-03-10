// Copyright (c) 2025 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

package com.dxfeed.sdk.symbol;

import org.graalvm.nativeimage.c.CContext;
import org.graalvm.nativeimage.c.struct.CField;
import org.graalvm.nativeimage.c.struct.CStruct;
import org.graalvm.nativeimage.c.type.CCharPointer;

@CContext(Directives.class)
@CStruct("dxfg_string_symbol_t")
public interface DxfgStringSymbol extends DxfgSymbol {

    @CField("symbol")
    CCharPointer getSymbol();

    @CField("symbol")
    void setSymbol(CCharPointer value);
}
