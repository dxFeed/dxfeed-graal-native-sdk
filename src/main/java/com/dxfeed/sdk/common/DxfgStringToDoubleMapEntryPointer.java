// Copyright (c) 2024 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

package com.dxfeed.sdk.common;

import org.graalvm.nativeimage.c.CContext;
import org.graalvm.nativeimage.c.struct.CField;
import org.graalvm.nativeimage.c.struct.CStruct;
import org.graalvm.nativeimage.c.type.CCharPointer;
import org.graalvm.nativeimage.c.type.CConst;
import org.graalvm.word.PointerBase;
import org.graalvm.word.SignedWord;

@CContext(Directives.class)
@CStruct("dxfg_string_to_double_map_entry_t")
public interface DxfgStringToDoubleMapEntryPointer extends PointerBase {

  @CField("key")
  @CConst
  CCharPointer getKey();

  @CField("key")
  void setKey(CCharPointer value);

  @CField("value")
  double getValue();

  @CField("value")
  void setValue(double value);

  DxfgStringToDoubleMapEntryPointer addressOf(int index);

  DxfgStringToDoubleMapEntryPointer addressOf(SignedWord index);
}
