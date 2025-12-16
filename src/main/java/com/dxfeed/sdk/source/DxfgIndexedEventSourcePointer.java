// Copyright (c) 2025 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

package com.dxfeed.sdk.source;

import org.graalvm.nativeimage.c.CContext;
import org.graalvm.nativeimage.c.struct.CField;
import org.graalvm.nativeimage.c.struct.CStruct;
import org.graalvm.nativeimage.c.type.CCharPointer;
import org.graalvm.word.PointerBase;

@CContext(Directives.class)
@CStruct("dxfg_indexed_event_source_t")
public interface DxfgIndexedEventSourcePointer extends PointerBase {

    @CField("type")
    int getType();

    @CField("type")
    void setType(final int value);

    @CField("id")
    int getId();

    @CField("id")
    void setId(final int value);

    @CField("name")
    CCharPointer getName();

    @CField("name")
    void setName(final CCharPointer name);
}
