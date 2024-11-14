// Copyright (c) 2024 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

package com.dxfeed.sdk.glossary;

import com.dxfeed.glossary.AdditionalUnderlyings;
import com.dxfeed.sdk.javac.JavaObjectHandler;
import org.graalvm.nativeimage.c.CContext;
import org.graalvm.nativeimage.c.struct.CStruct;

@CContext(Directives.class)
@CStruct("dxfg_additional_underlyings_t")
public interface DxfgAdditionalUnderlyingsHandle extends JavaObjectHandler<AdditionalUnderlyings> {

}
