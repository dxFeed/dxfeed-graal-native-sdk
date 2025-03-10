// Copyright (c) 2025 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

package com.dxfeed.sdk.events;

import com.dxfeed.sdk.javac.CPointerPointer;
import com.dxfeed.sdk.source.DxfgIndexedEventSource;
import org.graalvm.nativeimage.c.CContext;
import org.graalvm.nativeimage.c.struct.CPointerTo;

@CContext(Directives.class)
@CPointerTo(DxfgIndexedEventSource.class)
public interface DxfgIndexedEventSourcePointer extends CPointerPointer<DxfgIndexedEventSource> {

}
