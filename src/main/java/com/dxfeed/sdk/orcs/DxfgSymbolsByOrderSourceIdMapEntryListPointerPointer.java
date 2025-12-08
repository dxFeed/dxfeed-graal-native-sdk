// Copyright (c) 2025 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

package com.dxfeed.sdk.orcs;

import com.dxfeed.sdk.javac.CPointerPointer;
import org.graalvm.nativeimage.c.CContext;
import org.graalvm.nativeimage.c.struct.CPointerTo;

@CContext(Directives.class)
@CPointerTo(DxfgSymbolsByOrderSourceIdMapEntryListPointer.class)
public interface DxfgSymbolsByOrderSourceIdMapEntryListPointerPointer extends
        CPointerPointer<DxfgSymbolsByOrderSourceIdMapEntryListPointer> {

}
