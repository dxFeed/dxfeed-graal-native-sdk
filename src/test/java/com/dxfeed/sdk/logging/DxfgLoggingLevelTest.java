// Copyright (c) 2026 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

package com.dxfeed.sdk.logging;

import static org.junit.Assert.assertEquals;

import java.util.logging.Level;
import org.junit.Test;

public class DxfgLoggingLevelTest {

    // A level of its own: Level's constructor is protected.
    private static final class CustomLevel extends Level {
        private static final long serialVersionUID = 1L;

        CustomLevel(final int value) {
            super("CUSTOM" + value, value);
        }
    }

    @Test
    public void everySdkLevelMapsBackToItself() {
        for (final DxfgLoggingLevel level : DxfgLoggingLevel.values()) {
            assertEquals(level, DxfgLoggingLevel.of(level.getLevel()));
        }
    }

    @Test
    public void javaLevelsWithoutSdkConstantMapToTheNextLowerOne() {
        assertEquals(DxfgLoggingLevel.DXFG_LOGGING_LEVEL_TRACE, DxfgLoggingLevel.of(Level.FINER));
        assertEquals(DxfgLoggingLevel.DXFG_LOGGING_LEVEL_DEBUG, DxfgLoggingLevel.of(Level.CONFIG));
    }

    @Test
    public void customLevelsMapToTheNextLowerSdkLevel() {
        assertEquals(DxfgLoggingLevel.DXFG_LOGGING_LEVEL_TRACE, DxfgLoggingLevel.of(new CustomLevel(100)));
        assertEquals(DxfgLoggingLevel.DXFG_LOGGING_LEVEL_DEBUG, DxfgLoggingLevel.of(new CustomLevel(799)));
        assertEquals(DxfgLoggingLevel.DXFG_LOGGING_LEVEL_INFO, DxfgLoggingLevel.of(new CustomLevel(850)));
        assertEquals(DxfgLoggingLevel.DXFG_LOGGING_LEVEL_WARN, DxfgLoggingLevel.of(new CustomLevel(950)));
        assertEquals(DxfgLoggingLevel.DXFG_LOGGING_LEVEL_ERROR, DxfgLoggingLevel.of(new CustomLevel(1100)));
    }
}
