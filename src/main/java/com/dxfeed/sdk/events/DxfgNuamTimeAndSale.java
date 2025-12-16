// Copyright (c) 2025 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

package com.dxfeed.sdk.events;

import org.graalvm.nativeimage.c.CContext;
import org.graalvm.nativeimage.c.struct.CField;
import org.graalvm.nativeimage.c.struct.CStruct;
import org.graalvm.nativeimage.c.type.CCharPointer;

@CContext(Directives.class)
@CStruct("dxfg_nuam_time_and_sale_t")
public interface DxfgNuamTimeAndSale extends DxfgTimeAndSale {
    @CField("match_id")
    long getMatchId();

    @CField("match_id")
    void setMatchId(long matchId);
}
