// Copyright (c) 2025 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

package com.dxfeed.sdk.events;

import org.graalvm.nativeimage.c.CContext;
import org.graalvm.nativeimage.c.struct.CField;
import org.graalvm.nativeimage.c.struct.CStruct;
import org.graalvm.nativeimage.c.type.CCharPointer;

@CContext(Directives.class)
@CStruct("dxfg_spread_order_t")
public interface DxfgSpreadOrder extends DxfgOrderBase {

    @CField("spread_symbol")
    CCharPointer getSpreadSymbol();

    @CField("spread_symbol")
    void setSpreadSymbol(CCharPointer value);
}
