// Copyright (c) 2025 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

package com.dxfeed.sdk.common;

import org.graalvm.nativeimage.c.CContext;
import org.graalvm.nativeimage.c.struct.CPointerTo;
import org.graalvm.word.PointerBase;
import org.graalvm.word.SignedWord;

@CContext(Directives.class)
@CPointerTo(nameOfCType = "int32_t")
public interface CInt32Pointer extends PointerBase {

    int read();

    int read(int index);

    int read(SignedWord index);

    void write(int value);

    void write(int index, int value);

    void write(SignedWord index, int value);

    CInt32Pointer addressOf(int index);

    CInt32Pointer addressOf(SignedWord index);
}
