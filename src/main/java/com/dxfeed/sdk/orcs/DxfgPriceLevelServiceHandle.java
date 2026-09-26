// Copyright (c) 2025 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

package com.dxfeed.sdk.orcs;

import com.dxfeed.sdk.javac.JavaObjectHandler;
import org.graalvm.nativeimage.c.CContext;
import org.graalvm.nativeimage.c.struct.CStruct;

@CContext(OrcsDirectives.class)
@CStruct("dxfg_price_level_service_t")
public interface DxfgPriceLevelServiceHandle extends JavaObjectHandler<PriceLevelServiceHolder> {

}
