// Copyright (c) 2025 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

package com.dxfeed.sdk.orcs;

import com.dxfeed.sdk.javac.DxfgCStringListPointer;
import org.graalvm.nativeimage.c.CContext;
import org.graalvm.nativeimage.c.struct.CField;
import org.graalvm.nativeimage.c.struct.CStruct;
import org.graalvm.word.PointerBase;

@CContext(Directives.class)
@CStruct("dxfg_symbols_by_order_source_id_map_entry_t")
public interface DxfgSymbolsByOrderSourceIdMapEntryPointer extends PointerBase {
    @CField("order_source_id")
    int getOrderSourceId();

    @CField("order_source_id")
    void setOrderSourceId(int value);

    @CField("symbols")
    DxfgCStringListPointer getSymbols();

    @CField("symbols")
    void setSymbols(DxfgCStringListPointer value);
}
