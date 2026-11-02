// Copyright (c) 2026 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

package com.dxfeed.sdk.javac;

import org.graalvm.nativeimage.c.CContext;
import org.graalvm.nativeimage.c.struct.CField;
import org.graalvm.nativeimage.c.struct.CStruct;
import org.graalvm.nativeimage.c.type.CCharPointer;
import org.graalvm.word.PointerBase;

/**
 * {@code dxfg_v4_byte_list}: a list of bytes that holds the bytes themselves.
 */
@CContext(JavacV4Directives.class)
@CStruct("dxfg_v4_byte_list")
public interface DxfgByteList extends PointerBase {

    @CField("size")
    int getSize();

    @CField("size")
    void setSize(int value);

    @CField("elements")
    CCharPointer getElements();

    @CField("elements")
    void setElements(CCharPointer value);
}
