// Copyright (c) 2024 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

package com.dxfeed.sdk.glossary.mappers;

import com.dxfeed.glossary.AdditionalUnderlyings;
import com.dxfeed.sdk.glossary.DxfgAdditionalUnderlyingsHandle;
import com.dxfeed.sdk.mappers.JavaObjectHandlerMapper;
import org.graalvm.nativeimage.c.struct.SizeOf;

public class AdditionalUnderlyingsMapper extends
    JavaObjectHandlerMapper<AdditionalUnderlyings, DxfgAdditionalUnderlyingsHandle> {

  @Override
  protected int getSizeJavaObjectHandler() {
    return SizeOf.get(DxfgAdditionalUnderlyingsHandle.class);
  }
}
