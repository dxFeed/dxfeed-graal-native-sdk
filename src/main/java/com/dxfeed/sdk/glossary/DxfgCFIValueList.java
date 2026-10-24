// Copyright (c) 2026 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

package com.dxfeed.sdk.glossary;

import com.dxfeed.sdk.javac.CList;
import org.graalvm.nativeimage.c.CContext;
import org.graalvm.nativeimage.c.struct.CStruct;

/**
 * {@code dxfg_v4_cfi_value_list}: a list of CFI value handles.
 */
@CContext(GlossaryV4Directives.class)
@CStruct("dxfg_v4_cfi_value_list")
public interface DxfgCFIValueList extends CList<DxfgCFIValueHandlePointer> {

}
