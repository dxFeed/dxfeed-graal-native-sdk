// Copyright (c) 2025 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

package com.dxfeed.sdk.logging;

import java.util.logging.Level;
import org.graalvm.nativeimage.c.CContext;
import org.graalvm.nativeimage.c.constant.CEnum;
import org.graalvm.nativeimage.c.constant.CEnumLookup;
import org.graalvm.nativeimage.c.constant.CEnumValue;

@CContext(LoggingDirectives.class)
@CEnum("dxfg_logging_level_t")
public enum DxfgLoggingLevel {
    DXFG_LOGGING_LEVEL_ALL(Level.ALL),
    DXFG_LOGGING_LEVEL_TRACE(Level.FINEST),
    DXFG_LOGGING_LEVEL_DEBUG(Level.FINE),
    DXFG_LOGGING_LEVEL_INFO(Level.INFO),
    DXFG_LOGGING_LEVEL_WARN(Level.WARNING),
    DXFG_LOGGING_LEVEL_ERROR(Level.SEVERE),
    DXFG_LOGGING_LEVEL_OFF(Level.OFF),
    ;

    private final Level level;

    DxfgLoggingLevel(final Level level) {
        this.level = level;
    }

    /**
     * Returns the level a logging listener gets for a record of the given Java level: the highest SDK level that does
     * not exceed it, so the levels without an SDK constant map to the next lower one ({@code FINER} to TRACE,
     * {@code CONFIG} to DEBUG, a custom level between {@code WARNING} and {@code SEVERE} to WARN). {@code ALL} and
     * {@code OFF} map to themselves, a level below {@code FINEST} to TRACE.
     *
     * @param level the level of the record, not {@code null}
     * @return the SDK level, never {@code null}
     */
    public static DxfgLoggingLevel of(final Level level) {
        final int value = level.intValue();

        if (value == Level.OFF.intValue()) {
            return DXFG_LOGGING_LEVEL_OFF;
        }
        if (value == Level.ALL.intValue()) {
            return DXFG_LOGGING_LEVEL_ALL;
        }
        if (value >= Level.SEVERE.intValue()) {
            return DXFG_LOGGING_LEVEL_ERROR;
        }
        if (value >= Level.WARNING.intValue()) {
            return DXFG_LOGGING_LEVEL_WARN;
        }
        if (value >= Level.INFO.intValue()) {
            return DXFG_LOGGING_LEVEL_INFO;
        }
        if (value >= Level.FINE.intValue()) {
            return DXFG_LOGGING_LEVEL_DEBUG;
        }

        return DXFG_LOGGING_LEVEL_TRACE;
    }

    @CEnumLookup
    public static native DxfgLoggingLevel fromCValue(int value);

    @CEnumValue
    public native int getCValue();

    public Level getLevel() {
        return level;
    }
}
