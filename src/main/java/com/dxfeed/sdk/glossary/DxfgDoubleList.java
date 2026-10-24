// Copyright (c) 2026 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

package com.dxfeed.sdk.glossary;

import com.dxfeed.sdk.common.CDoublePointerPointer;
import com.dxfeed.sdk.common.CInt32Pointer;
import org.graalvm.nativeimage.c.CContext;
import org.graalvm.nativeimage.c.struct.CField;
import org.graalvm.nativeimage.c.struct.CFieldAddress;
import org.graalvm.nativeimage.c.struct.CStruct;
import org.graalvm.nativeimage.c.type.CDoublePointer;
import org.graalvm.word.PointerBase;

/**
 * {@code dxfg_v4_double_list}: a list of doubles that holds the values themselves.
 */
@CContext(GlossaryV4Directives.class)
@CStruct("dxfg_v4_double_list")
public interface DxfgDoubleList extends PointerBase {

    @CField("size")
    int getSize();

    @CField("size")
    void setSize(int value);

    @CField("elements")
    CDoublePointer getElements();

    @CField("elements")
    void setElements(CDoublePointer value);

    @CFieldAddress("size")
    CInt32Pointer addressOfSize();

    @CFieldAddress("elements")
    CDoublePointerPointer addressOfElements();
}
