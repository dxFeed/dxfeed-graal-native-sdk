// Copyright (c) 2024 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

package com.dxfeed.sdk.glossary.mappers;

import com.dxfeed.glossary.CFI;
import com.dxfeed.sdk.glossary.DxfgCFIValueHandle;
import com.dxfeed.sdk.mappers.JavaObjectHandlerMapper;
import org.graalvm.nativeimage.c.struct.SizeOf;

public class CFIValueMapper extends
    JavaObjectHandlerMapper<CFI.Value, DxfgCFIValueHandle> {

  @Override
  protected int getSizeJavaObjectHandler() {
    return SizeOf.get(DxfgCFIValueHandle.class);
  }
}
