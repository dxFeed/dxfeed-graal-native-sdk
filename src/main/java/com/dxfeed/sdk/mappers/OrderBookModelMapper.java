// Copyright (c) 2025 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

package com.dxfeed.sdk.mappers;

import com.dxfeed.model.market.OrderBookModel;
import com.dxfeed.sdk.model.DxfgOrderBookModel;
import org.graalvm.nativeimage.c.struct.SizeOf;

public class OrderBookModelMapper
        extends JavaObjectHandlerMapper<OrderBookModel, DxfgOrderBookModel> {

    @Override
    protected int getSizeJavaObjectHandler() {
        return SizeOf.get(DxfgOrderBookModel.class);
    }
}
