// Copyright (c) 2026 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

package com.dxfeed.sdk.ondemand;

import static org.junit.Assert.assertEquals;

import java.util.Date;
import org.junit.Test;

/**
 * {@link OnDemandPropertyValueKind}: the kinds of the values that OnDemandService reports, null and the other classes.
 */
public class OnDemandPropertyValueKindTest {

    @Test
    public void nullHasTheKindNull() {
        assertEquals(OnDemandPropertyValueKind.NULL, OnDemandPropertyValueKind.of(null));
    }

    @Test
    public void theValuesOfTheServiceHaveTheirKinds() {
        assertEquals(OnDemandPropertyValueKind.BOOLEAN, OnDemandPropertyValueKind.of(Boolean.TRUE));
        assertEquals(OnDemandPropertyValueKind.BOOLEAN, OnDemandPropertyValueKind.of(Boolean.FALSE));
        assertEquals(OnDemandPropertyValueKind.DOUBLE, OnDemandPropertyValueKind.of(1.5));
        assertEquals(OnDemandPropertyValueKind.TIME, OnDemandPropertyValueKind.of(new Date(0)));
    }

    @Test
    public void anotherClassHasTheKindOther() {
        assertEquals(OnDemandPropertyValueKind.OTHER, OnDemandPropertyValueKind.of("speed"));
        assertEquals(OnDemandPropertyValueKind.OTHER, OnDemandPropertyValueKind.of(1.5f));
        assertEquals(OnDemandPropertyValueKind.OTHER, OnDemandPropertyValueKind.of(1L));
    }

    @Test
    public void theKindsAreTheValuesOfTheHeader() {
        // dxfg_v4_on_demand_property_value_kind_t of dxfg_v4_ondemand.h.
        assertEquals(0, OnDemandPropertyValueKind.NULL);
        assertEquals(1, OnDemandPropertyValueKind.BOOLEAN);
        assertEquals(2, OnDemandPropertyValueKind.DOUBLE);
        assertEquals(3, OnDemandPropertyValueKind.TIME);
        assertEquals(4, OnDemandPropertyValueKind.OTHER);
    }
}
