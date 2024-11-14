// Copyright (c) 2024 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

package com.dxfeed.sdk.javac;

import org.graalvm.nativeimage.c.CContext;
import org.graalvm.nativeimage.c.struct.CPointerTo;
import org.graalvm.nativeimage.c.type.CCharPointer;

@CContext(Directives.class)
@CPointerTo(CCharPointer.class)
public interface DxfgCharPointerPointer extends CPointerPointer<CCharPointer> {

}
