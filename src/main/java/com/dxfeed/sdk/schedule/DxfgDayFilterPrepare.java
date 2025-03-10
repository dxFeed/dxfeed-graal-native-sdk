// Copyright (c) 2025 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

package com.dxfeed.sdk.schedule;

import com.dxfeed.schedule.DayFilter;
import java.util.HashMap;
import java.util.Map;
import org.graalvm.nativeimage.c.CContext;
import org.graalvm.nativeimage.c.constant.CEnum;
import org.graalvm.nativeimage.c.constant.CEnumLookup;
import org.graalvm.nativeimage.c.constant.CEnumValue;

@CContext(Directives.class)
@CEnum("dxfg_day_filter_prepare_t")
public enum DxfgDayFilterPrepare {
    DXFG_DAY_FILTER_ANY(DayFilter.ANY),
    DXFG_DAY_FILTER_TRADING(DayFilter.TRADING),
    DXFG_DAY_FILTER_NON_TRADING(DayFilter.NON_TRADING),
    DXFG_DAY_FILTER_HOLIDAY(DayFilter.HOLIDAY),
    DXFG_DAY_FILTER_SHORT_DAY(DayFilter.SHORT_DAY),
    DXFG_DAY_FILTER_MONDAY(DayFilter.MONDAY),
    DXFG_DAY_FILTER_TUESDAY(DayFilter.TUESDAY),
    DXFG_DAY_FILTER_WEDNESDAY(DayFilter.WEDNESDAY),
    DXFG_DAY_FILTER_THURSDAY(DayFilter.THURSDAY),
    DXFG_DAY_FILTER_FRIDAY(DayFilter.FRIDAY),
    DXFG_DAY_FILTER_SATURDAY(DayFilter.SATURDAY),
    DXFG_DAY_FILTER_SUNDAY(DayFilter.SUNDAY),
    DXFG_DAY_FILTER_WEEK_DAY(DayFilter.WEEK_DAY),
    DXFG_DAY_FILTER_WEEK_END(DayFilter.WEEK_END);

    private static final Map<DayFilter, DxfgDayFilterPrepare> map = new HashMap<>();

    static {
        map.put(DayFilter.ANY, DXFG_DAY_FILTER_ANY);
        map.put(DayFilter.TRADING, DXFG_DAY_FILTER_TRADING);
        map.put(DayFilter.NON_TRADING, DXFG_DAY_FILTER_NON_TRADING);
        map.put(DayFilter.HOLIDAY, DXFG_DAY_FILTER_HOLIDAY);
        map.put(DayFilter.SHORT_DAY, DXFG_DAY_FILTER_SHORT_DAY);
        map.put(DayFilter.MONDAY, DXFG_DAY_FILTER_MONDAY);
        map.put(DayFilter.TUESDAY, DXFG_DAY_FILTER_TUESDAY);
        map.put(DayFilter.WEDNESDAY, DXFG_DAY_FILTER_WEDNESDAY);
        map.put(DayFilter.THURSDAY, DXFG_DAY_FILTER_THURSDAY);
        map.put(DayFilter.FRIDAY, DXFG_DAY_FILTER_FRIDAY);
        map.put(DayFilter.SATURDAY, DXFG_DAY_FILTER_SATURDAY);
        map.put(DayFilter.SUNDAY, DXFG_DAY_FILTER_SUNDAY);
        map.put(DayFilter.WEEK_DAY, DXFG_DAY_FILTER_WEEK_DAY);
        map.put(DayFilter.WEEK_END, DXFG_DAY_FILTER_WEEK_END);
    }

    public final DayFilter dayFilter;

    DxfgDayFilterPrepare(final DayFilter dayFilter) {
        this.dayFilter = dayFilter;
    }

    public static DxfgDayFilterPrepare of(final DayFilter filter) {
        return map.get(filter);
    }

    @CEnumLookup
    public static native DxfgDayFilterPrepare fromCValue(int value);

    @CEnumValue
    public native int getCValue();
}
