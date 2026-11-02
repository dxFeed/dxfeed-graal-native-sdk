// Copyright (c) 2026 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

package com.dxfeed.sdk.schedule;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * {@link NullableBoolean}: -1 is null, 0 is false, 1 is true, any other value fails.
 */
public class NullableBooleanTest {

    @Test
    public void mapsTheThreeValues() {
        assertNull(NullableBoolean.fromC(-1, "trading"));
        assertEquals(Boolean.FALSE, NullableBoolean.fromC(0, "trading"));
        assertEquals(Boolean.TRUE, NullableBoolean.fromC(1, "trading"));
    }

    @Test
    public void rejectsTheOtherValues() {
        for (final int value : new int[] {2, -2, Integer.MIN_VALUE, Integer.MAX_VALUE}) {
            final IllegalArgumentException exception =
                    assertThrows(IllegalArgumentException.class, () -> NullableBoolean.fromC(value, "holiday"));

            assertTrue(exception.getMessage(), exception.getMessage().contains("`holiday`"));
            assertTrue(exception.getMessage(), exception.getMessage().contains(Integer.toString(value)));
        }
    }
}
