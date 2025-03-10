// Copyright (c) 2025 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

package com.dxfeed.sdk.mappers;

import com.dxfeed.model.market.OrderBookModelListener;
import com.dxfeed.sdk.model.DxfgOrderBookModelListener;
import org.graalvm.nativeimage.c.struct.SizeOf;

public class OrderBookModelListenerMapper
        extends JavaObjectHandlerMapper<OrderBookModelListener, DxfgOrderBookModelListener> {

    @Override
    protected int getSizeJavaObjectHandler() {
        return SizeOf.get(DxfgOrderBookModelListener.class);
    }
}
