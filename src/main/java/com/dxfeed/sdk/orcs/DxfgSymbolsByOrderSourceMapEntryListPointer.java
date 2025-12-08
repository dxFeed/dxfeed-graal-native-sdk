// Copyright (c) 2025 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

package com.dxfeed.sdk.orcs;

import com.dxfeed.sdk.javac.CList;
import org.graalvm.nativeimage.c.CContext;
import org.graalvm.nativeimage.c.struct.CStruct;

@CContext(Directives.class)
@CStruct("dxfg_symbols_by_order_source_map_entry_list_t")
public interface DxfgSymbolsByOrderSourceMapEntryListPointer extends
        CList<DxfgSymbolsByOrderSourceMapEntryPointerPointer> {

}
