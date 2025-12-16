// Copyright (c) 2025 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

package com.dxfeed.sdk.events;

import org.graalvm.nativeimage.c.CContext;
import org.graalvm.nativeimage.c.struct.CField;
import org.graalvm.nativeimage.c.struct.CStruct;

@CContext(Directives.class)
@CStruct("dxfg_order_imbalance_t")
public interface DxfgOrderImbalance extends DxfgMarketEvent {

    @CField("source_id")
    int getSourceId();

    @CField("source_id")
    void setSourceId(int sourceId);

    @CField("time_sequence")
    long getTimeSequence();

    @CField("time_sequence")
    void setTimeSequence(long timeSequence);

    @CField("ref_price")
    double getRefPrice();

    @CField("ref_price")
    void setRefPrice(double refPrice);

    @CField("paired_size")
    double getPairedSize();

    @CField("paired_size")
    void setPairedSize(double pairedSize);

    @CField("imbalance_size")
    double getImbalanceSize();

    @CField("imbalance_size")
    void setImbalanceSize(double imbalanceSize);

    @CField("near_price")
    double getNearPrice();

    @CField("near_price")
    void setNearPrice(double nearPrice);

    @CField("far_price")
    double getFarPrice();

    @CField("far_price")
    void setFarPrice(double farPrice);

    @CField("flags")
    int getFlags();

    @CField("flags")
    void setFlags(int flags);
}
