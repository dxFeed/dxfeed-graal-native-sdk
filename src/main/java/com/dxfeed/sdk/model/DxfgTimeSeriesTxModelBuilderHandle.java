// Copyright (c) 2026 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

package com.dxfeed.sdk.model;

import com.dxfeed.sdk.javac.JavaObjectHandler;
import com.dxfeed.sdk.model.tx.TimeSeriesTxModel;
import org.graalvm.nativeimage.c.CContext;
import org.graalvm.nativeimage.c.struct.CStruct;

@CContext(ModelDirectives.class)
@CStruct("dxfg_time_series_tx_model_builder_t")
public interface DxfgTimeSeriesTxModelBuilderHandle extends JavaObjectHandler<TimeSeriesTxModel.Builder<?>> {
}
