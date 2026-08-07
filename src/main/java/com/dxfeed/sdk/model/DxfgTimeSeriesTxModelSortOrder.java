// Copyright (c) 2025 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

package com.dxfeed.sdk.model;

import com.dxfeed.api.experimental.model.TimeSeriesTxModel;
import org.graalvm.nativeimage.c.CContext;
import org.graalvm.nativeimage.c.constant.CEnum;
import org.graalvm.nativeimage.c.constant.CEnumLookup;
import org.graalvm.nativeimage.c.constant.CEnumValue;

@CContext(Directives.class)
@CEnum("dxfg_TimeSeriesTxModelSortOrder_t")
public enum DxfgTimeSeriesTxModelSortOrder {
    NONE(TimeSeriesTxModel.SortOrder.NONE),
    ASCENDING(TimeSeriesTxModel.SortOrder.ASCENDING),
    DESCENDING(TimeSeriesTxModel.SortOrder.DESCENDING);

    public final TimeSeriesTxModel.SortOrder sortOrder;

    DxfgTimeSeriesTxModelSortOrder(final TimeSeriesTxModel.SortOrder sortOrder) {
        this.sortOrder = sortOrder;
    }

    @CEnumLookup
    public static native DxfgTimeSeriesTxModelSortOrder fromCValue(int value);

    @CEnumValue
    public native int getCValue();
}
