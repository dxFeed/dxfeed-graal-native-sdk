// Copyright (c) 2024 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

package com.dxfeed.sdk.common;

import com.dxfeed.sdk.javac.CPointerPointer;
import org.graalvm.nativeimage.c.CContext;
import org.graalvm.nativeimage.c.struct.CPointerTo;
import org.graalvm.nativeimage.c.type.CDoublePointer;

@CContext(Directives.class)
@CPointerTo(CDoublePointer.class)
public interface CDoublePointerPointer extends
    CPointerPointer<CDoublePointer> {
}
