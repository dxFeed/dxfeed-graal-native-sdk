// Copyright (c) 2026 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

package com.dxfeed.sdk.glossary;

import com.dxfeed.sdk.common.CInt32Pointer;
import com.dxfeed.sdk.common.DxfgStringToDoubleMapEntryPointer;
import com.dxfeed.sdk.common.DxfgStringToDoubleMapEntryPointerPointer;
import org.graalvm.nativeimage.c.CContext;
import org.graalvm.nativeimage.c.struct.CField;
import org.graalvm.nativeimage.c.struct.CFieldAddress;
import org.graalvm.nativeimage.c.struct.CStruct;
import org.graalvm.word.PointerBase;

/**
 * {@code dxfg_v4_string_to_double_map_entry_list}: a list of string to double map entries that holds the entries
 * themselves.
 */
@CContext(GlossaryV4Directives.class)
@CStruct("dxfg_v4_string_to_double_map_entry_list")
public interface DxfgStringToDoubleMapEntryList extends PointerBase {

    @CField("size")
    int getSize();

    @CField("size")
    void setSize(int value);

    @CField("elements")
    DxfgStringToDoubleMapEntryPointer getElements();

    @CField("elements")
    void setElements(DxfgStringToDoubleMapEntryPointer value);

    @CFieldAddress("size")
    CInt32Pointer addressOfSize();

    @CFieldAddress("elements")
    DxfgStringToDoubleMapEntryPointerPointer addressOfElements();
}
