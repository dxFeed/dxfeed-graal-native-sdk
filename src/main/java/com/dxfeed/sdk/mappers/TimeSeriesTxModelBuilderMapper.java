// Copyright (c) 2026 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

package com.dxfeed.sdk.mappers;

import com.dxfeed.sdk.model.DxfgTimeSeriesTxModelBuilderHandle;
import com.dxfeed.sdk.model.tx.TimeSeriesTxModel;
import org.graalvm.nativeimage.c.struct.SizeOf;

public class TimeSeriesTxModelBuilderMapper
        extends JavaObjectHandlerMapper<TimeSeriesTxModel.Builder<?>, DxfgTimeSeriesTxModelBuilderHandle> {

    @Override
    protected int getSizeJavaObjectHandler() {
        return SizeOf.get(DxfgTimeSeriesTxModelBuilderHandle.class);
    }
}
