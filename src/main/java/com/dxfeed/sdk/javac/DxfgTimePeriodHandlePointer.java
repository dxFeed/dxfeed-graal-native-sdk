// Copyright (c) 2025 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

package com.dxfeed.sdk.javac;

import org.graalvm.nativeimage.c.CContext;
import org.graalvm.nativeimage.c.struct.CPointerTo;

@CContext(Directives.class)
@CPointerTo(DxfgTimePeriodHandle.class)
public interface DxfgTimePeriodHandlePointer extends CPointerPointer<DxfgTimePeriodHandle> {

}
