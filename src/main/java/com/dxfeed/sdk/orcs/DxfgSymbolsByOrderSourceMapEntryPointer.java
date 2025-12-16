// Copyright (c) 2025 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

package com.dxfeed.sdk.orcs;

import com.dxfeed.sdk.javac.DxfgCStringListPointer;
import com.dxfeed.sdk.source.DxfgIndexedEventSourcePointer;
import org.graalvm.nativeimage.c.CContext;
import org.graalvm.nativeimage.c.struct.CField;
import org.graalvm.nativeimage.c.struct.CStruct;
import org.graalvm.word.PointerBase;

@CContext(Directives.class)
@CStruct("dxfg_symbols_by_order_source_map_entry_t")
public interface DxfgSymbolsByOrderSourceMapEntryPointer extends PointerBase {
    @CField("order_source")
    DxfgIndexedEventSourcePointer getOrderSource();

    @CField("order_source")
    void setOrderSource(DxfgIndexedEventSourcePointer value);

    @CField("symbols")
    DxfgCStringListPointer getSymbols();

    @CField("symbols")
    void setSymbols(DxfgCStringListPointer value);
}
