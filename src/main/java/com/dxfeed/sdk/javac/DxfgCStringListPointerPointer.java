// Copyright (c) 2025 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

package com.dxfeed.sdk.javac;

import org.graalvm.nativeimage.c.CContext;
import org.graalvm.nativeimage.c.struct.CPointerTo;

@CContext(JavacDirectives.class)
@CPointerTo(DxfgCStringListPointer.class)
public interface DxfgCStringListPointerPointer extends CPointerPointer<DxfgCStringListPointer> {

}
