// Copyright (c) 2025 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

package com.dxfeed.sdk.common;

import org.graalvm.nativeimage.c.CContext;
import org.graalvm.nativeimage.c.struct.CPointerTo;
import org.graalvm.nativeimage.c.type.CCharPointerPointer;
import org.graalvm.word.PointerBase;

@CContext(Directives.class)
@CPointerTo(CCharPointerPointer.class)
public interface CCharPointerPointerPointer extends PointerBase {

    CCharPointerPointerPointer addressOf(int index);

    CCharPointerPointer read();

    void write(CCharPointerPointer data);
}
