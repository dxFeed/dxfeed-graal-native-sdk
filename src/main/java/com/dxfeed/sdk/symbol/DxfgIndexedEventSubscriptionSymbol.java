// Copyright (c) 2025 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

package com.dxfeed.sdk.symbol;

import com.dxfeed.sdk.source.DxfgIndexedEventSource;
import org.graalvm.nativeimage.c.CContext;
import org.graalvm.nativeimage.c.struct.CField;
import org.graalvm.nativeimage.c.struct.CStruct;

@CContext(Directives.class)
@CStruct("dxfg_indexed_event_subscription_symbol_t")
public interface DxfgIndexedEventSubscriptionSymbol extends DxfgSymbol {

    @CField("symbol")
    DxfgSymbol getSymbol();

    @CField("symbol")
    void setSymbol(DxfgSymbol value);

    @CField("source")
    DxfgIndexedEventSource getSource();

    @CField("source")
    void setSource(DxfgIndexedEventSource value);
}
