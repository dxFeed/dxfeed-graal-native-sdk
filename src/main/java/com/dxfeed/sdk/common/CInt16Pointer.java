// Copyright (c) 2024 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

package com.dxfeed.sdk.common;

import org.graalvm.nativeimage.c.CContext;
import org.graalvm.nativeimage.c.struct.CPointerTo;
import org.graalvm.word.PointerBase;
import org.graalvm.word.SignedWord;

@CContext(Directives.class)
@CPointerTo(nameOfCType = "int16_t")
public interface CInt16Pointer extends PointerBase {

  short read();

  short read(int index);

  short read(SignedWord index);

  void write(short value);

  void write(char value);

  void write(int index, short value);

  void write(int index, char value);

  void write(SignedWord index, short value);

  void write(SignedWord index, char value);

  CInt16Pointer addressOf(int index);

  CInt16Pointer addressOf(SignedWord index);
}
