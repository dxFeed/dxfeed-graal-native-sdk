// Copyright (c) 2024 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

package com.dxfeed.sdk.javac;

import org.graalvm.nativeimage.c.CContext;
import org.graalvm.nativeimage.c.struct.CPointerTo;
import org.graalvm.nativeimage.c.type.VoidPointer;
import org.graalvm.word.PointerBase;

@CContext(Directives.class)
@CPointerTo(VoidPointer.class)
public interface CPointerPointer<T extends PointerBase> extends PointerBase {

  CPointerPointer<T> addressOf(int index);

  T read();

  void write(T data);
}
