// Copyright (c) 2026 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

package com.dxfeed.sdk.schedule;

/**
 * A nullable {@link Boolean} that comes from C as an {@code int32_t}: -1 is {@code null}, 0 is {@code false}, 1 is
 * {@code true} (the filter conditions of {@code dxfg_v4_DayFilter_newWithUserData} and
 * {@code dxfg_v4_SessionFilter_newWithUserData}).
 */
public final class NullableBoolean {

    /** The C value of {@code null}: the condition is not checked. */
    public static final int ANY = -1;

    private NullableBoolean() {
    }

    /**
     * @param value the C value: -1, 0 or 1.
     * @param name  the name of the parameter, for the message of the exception.
     * @return {@code null}, {@code false} or {@code true}.
     * @throws IllegalArgumentException if the value is not -1, 0 or 1.
     */
    public static Boolean fromC(final int value, final String name) {
        switch (value) {
            case ANY:
                return null;
            case 0:
                return Boolean.FALSE;
            case 1:
                return Boolean.TRUE;
            default:
                throw new IllegalArgumentException(
                        "The `" + name + "` value " + value + " is not -1 (any), 0 (false) or 1 (true)");
        }
    }
}
