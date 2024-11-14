// Copyright (c) 2024 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

package com.dxfeed.sdk.glossary;

import com.dxfeed.glossary.CFI;
import com.dxfeed.sdk.common.DxfgStringToDoubleMapEntryPointer;
import com.dxfeed.sdk.javac.JavaObjectHandler;
import org.graalvm.nativeimage.c.CContext;
import org.graalvm.nativeimage.c.struct.CStruct;
import org.graalvm.word.SignedWord;

@CContext(Directives.class)
@CStruct("dxfg_cfi_value_t")
public interface DxfgCFIValueHandle extends JavaObjectHandler<CFI.Value> {
}
