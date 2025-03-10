// Copyright (c) 2025 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

package com.dxfeed.sdk.events;

import com.dxfeed.sdk.javac.CPointerPointer;
import org.graalvm.nativeimage.c.CContext;
import org.graalvm.nativeimage.c.struct.CPointerTo;
import org.graalvm.nativeimage.c.type.CIntPointer;

@CContext(Directives.class)
@CPointerTo(CIntPointer.class)
public interface DxfgEventClazzPointer extends CPointerPointer<CIntPointer> {

}
