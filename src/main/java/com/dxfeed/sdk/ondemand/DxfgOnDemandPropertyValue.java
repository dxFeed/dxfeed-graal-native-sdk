// Copyright (c) 2026 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

package com.dxfeed.sdk.ondemand;

import org.graalvm.nativeimage.c.CContext;
import org.graalvm.nativeimage.c.struct.CField;
import org.graalvm.nativeimage.c.struct.CStruct;
import org.graalvm.word.PointerBase;

/**
 * The old or the new value of an on-demand service property ({@code dxfg_v4_on_demand_property_value_t}).
 */
@CContext(OnDemandV4Directives.class)
@CStruct("dxfg_v4_on_demand_property_value_t")
public interface DxfgOnDemandPropertyValue extends PointerBase {

    // The kind is one of OnDemandPropertyValueKind.
    @CField("kind")
    int getKind();

    @CField("kind")
    void setKind(int value);

    @CField("boolean_value")
    int getBooleanValue();

    @CField("boolean_value")
    void setBooleanValue(int value);

    @CField("double_value")
    double getDoubleValue();

    @CField("double_value")
    void setDoubleValue(double value);

    @CField("time_value")
    long getTimeValue();

    @CField("time_value")
    void setTimeValue(long value);
}
