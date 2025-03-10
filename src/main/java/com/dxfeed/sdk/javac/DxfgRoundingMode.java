// Copyright (c) 2025 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

package com.dxfeed.sdk.javac;

import java.math.RoundingMode;
import java.util.EnumMap;
import java.util.Map;
import org.graalvm.nativeimage.c.CContext;
import org.graalvm.nativeimage.c.constant.CEnum;
import org.graalvm.nativeimage.c.constant.CEnumLookup;
import org.graalvm.nativeimage.c.constant.CEnumValue;

@CContext(Directives.class)
@CEnum("dxfg_rounding_mode_t")
public enum DxfgRoundingMode {
    DXFG_ROUNDING_MODE_UP(RoundingMode.UP),
    DXFG_ROUNDING_MODE_DOWN(RoundingMode.DOWN),
    DXFG_ROUNDING_MODE_CEILING(RoundingMode.CEILING),
    DXFG_ROUNDING_MODE_FLOOR(RoundingMode.FLOOR),
    DXFG_ROUNDING_MODE_HALF_UP(RoundingMode.HALF_UP),
    DXFG_ROUNDING_MODE_HALF_DOWN(RoundingMode.HALF_DOWN),
    DXFG_ROUNDING_MODE_HALF_EVEN(RoundingMode.HALF_EVEN),
    DXFG_ROUNDING_MODE_UNNECESSARY(RoundingMode.UNNECESSARY),
    ;

    private static final Map<RoundingMode, DxfgRoundingMode> map = new EnumMap<>(RoundingMode.class);

    static {
        map.put(RoundingMode.UP, DXFG_ROUNDING_MODE_UP);
        map.put(RoundingMode.DOWN, DXFG_ROUNDING_MODE_DOWN);
        map.put(RoundingMode.CEILING, DXFG_ROUNDING_MODE_CEILING);
        map.put(RoundingMode.FLOOR, DXFG_ROUNDING_MODE_FLOOR);
        map.put(RoundingMode.HALF_UP, DXFG_ROUNDING_MODE_HALF_UP);
        map.put(RoundingMode.HALF_DOWN, DXFG_ROUNDING_MODE_HALF_DOWN);
        map.put(RoundingMode.HALF_EVEN, DXFG_ROUNDING_MODE_HALF_EVEN);
        map.put(RoundingMode.UNNECESSARY, DXFG_ROUNDING_MODE_UNNECESSARY);
    }

    private final RoundingMode roundingMode;

    DxfgRoundingMode(final RoundingMode roundingMode) {
        this.roundingMode = roundingMode;
    }

    public static DxfgRoundingMode of(final RoundingMode mode) {
        return map.get(mode);
    }

    @CEnumLookup
    public static native DxfgRoundingMode fromCValue(int value);

    @CEnumValue
    public native int getCValue();

    public RoundingMode getRoundingMode() {
        return roundingMode;
    }
}
