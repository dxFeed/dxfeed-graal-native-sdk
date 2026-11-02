// Copyright (c) 2026 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

package com.dxfeed.sdk.ondemand;

import java.util.Date;

/**
 * The kinds of the value of an on-demand service property: the values of
 * {@code dxfg_v4_on_demand_property_value_kind_t} of dxfg_v4_ondemand.h, and the kind of a Java value of a property
 * change.
 */
public final class OnDemandPropertyValueKind {

    public static final int NULL = 0;
    public static final int BOOLEAN = 1;
    public static final int DOUBLE = 2;
    public static final int TIME = 3;
    public static final int OTHER = 4;

    private OnDemandPropertyValueKind() {
    }

    /**
     * @param value the old or the new value of a property change
     * @return the kind of the value: {@link #NULL} for {@code null}, {@link #BOOLEAN}, {@link #DOUBLE}, {@link #TIME}
     *         for a {@link Date}, {@link #OTHER} for a value of another class
     */
    public static int of(final Object value) {
        if (value == null) {
            return NULL;
        }

        if (value instanceof Boolean) {
            return BOOLEAN;
        }

        if (value instanceof Double) {
            return DOUBLE;
        }

        if (value instanceof Date) {
            return TIME;
        }

        return OTHER;
    }
}
