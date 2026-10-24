// Copyright (c) 2026 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

package com.dxfeed.sdk.schedule;

import com.dxfeed.sdk.NativeUtils;
import com.dxfeed.sdk.common.CInt32Pointer;
import com.dxfeed.sdk.common.DxfgOut;
import com.dxfeed.sdk.exception.ExceptionHandlerReturnMinusOne;
import com.dxfeed.sdk.exception.OlderEntryPoint;
import com.dxfeed.sdk.ipf.DxfgInstrumentProfile2Pointer;
import com.dxfeed.sdk.ipf.DxfgInstrumentProfile;
import com.dxfeed.sdk.javac.DxfgCStringListPointer;
import com.dxfeed.sdk.javac.DxfgCStringListPointerPointer;
import java.io.IOException;
import org.graalvm.nativeimage.IsolateThread;
import org.graalvm.nativeimage.c.CContext;
import org.graalvm.nativeimage.c.function.CEntryPoint;
import org.graalvm.nativeimage.c.type.CCharPointer;
import org.graalvm.nativeimage.c.type.CCharPointerPointer;
import org.graalvm.nativeimage.c.type.CConst;
import org.graalvm.nativeimage.c.type.CLongPointer;
import org.graalvm.nativeimage.c.type.WordPointer;

/**
 * The v4 twins of the functions of {@link ScheduleNative}: each one calls the old function as a Java method
 * ({@link OlderEntryPoint} tells whether it failed) and returns its result through an out parameter (see
 * dxfg_v4_api.h).
 */
@CContext(ScheduleDirectives.class)
public final class ScheduleV4Native {

    private ScheduleV4Native() {
    }

    @CEntryPoint(
            name = "dxfg_v4_SessionFilter_new",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_SessionFilter_new(
            final IsolateThread thread,
            final DxfgSessionFilterFunction dxfgSessionFilterFunction,
            @DxfgOut final WordPointer sessionFilter
    ) {
        if (sessionFilter.isNull()) {
            throw new IllegalArgumentException("The `sessionFilter` pointer is null");
        }

        final Throwable stored = OlderEntryPoint.takeStoredException();
        final DxfgSessionFilter result = ScheduleNative.dxfg_SessionFilter_new(thread, dxfgSessionFilterFunction);

        if (OlderEntryPoint.failed(stored, result.isNull())) {
            return ExceptionHandlerReturnMinusOne.EXECUTE_FAIL;
        }

        sessionFilter.write(result);

        return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
    }

    @CEntryPoint(
            name = "dxfg_v4_SessionFilter_getInstance",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_SessionFilter_getInstance(
            final IsolateThread thread,
            final DxfgSessionFilterPrepare dxfgSessionFilterPrepare,
            @DxfgOut final WordPointer sessionFilter
    ) {
        if (sessionFilter.isNull()) {
            throw new IllegalArgumentException("The `sessionFilter` pointer is null");
        }

        final Throwable stored = OlderEntryPoint.takeStoredException();
        final DxfgSessionFilter result = ScheduleNative.dxfg_SessionFilter_getInstance(thread, dxfgSessionFilterPrepare);

        if (OlderEntryPoint.failed(stored, result.isNull())) {
            return ExceptionHandlerReturnMinusOne.EXECUTE_FAIL;
        }

        sessionFilter.write(result);

        return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
    }

    @CEntryPoint(
            name = "dxfg_v4_DayFilter_new",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_DayFilter_new(
            final IsolateThread thread,
            final DxfgDayFilterFunction dxfgDayFilterFunction,
            @DxfgOut final WordPointer dayFilter
    ) {
        if (dayFilter.isNull()) {
            throw new IllegalArgumentException("The `dayFilter` pointer is null");
        }

        final Throwable stored = OlderEntryPoint.takeStoredException();
        final DxfgDayFilter result = ScheduleNative.dxfg_DayFilter_new(thread, dxfgDayFilterFunction);

        if (OlderEntryPoint.failed(stored, result.isNull())) {
            return ExceptionHandlerReturnMinusOne.EXECUTE_FAIL;
        }

        dayFilter.write(result);

        return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
    }

    @CEntryPoint(
            name = "dxfg_v4_DayFilter_getInstance",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_DayFilter_getInstance(
            final IsolateThread thread,
            final DxfgDayFilterPrepare dxfgDayFilterPrepare,
            @DxfgOut final WordPointer dayFilter
    ) {
        if (dayFilter.isNull()) {
            throw new IllegalArgumentException("The `dayFilter` pointer is null");
        }

        final Throwable stored = OlderEntryPoint.takeStoredException();
        final DxfgDayFilter result = ScheduleNative.dxfg_DayFilter_getInstance(thread, dxfgDayFilterPrepare);

        if (OlderEntryPoint.failed(stored, result.isNull())) {
            return ExceptionHandlerReturnMinusOne.EXECUTE_FAIL;
        }

        dayFilter.write(result);

        return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
    }

    @CEntryPoint(
            name = "dxfg_v4_Schedule_getInstanceByProfileHandle",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_Schedule_getInstanceByProfileHandle(
            final IsolateThread thread,
            final DxfgInstrumentProfile dxfgInstrumentProfile,
            @DxfgOut final WordPointer schedule
    ) {
        if (schedule.isNull()) {
            throw new IllegalArgumentException("The `schedule` pointer is null");
        }

        final Throwable stored = OlderEntryPoint.takeStoredException();
        final DxfgScheduleHandle result = ScheduleNative.dxfg_Schedule_getInstance(thread, dxfgInstrumentProfile);

        if (OlderEntryPoint.failed(stored, result.isNull())) {
            return ExceptionHandlerReturnMinusOne.EXECUTE_FAIL;
        }

        schedule.write(result);

        return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
    }

    @CEntryPoint(
            name = "dxfg_v4_Schedule_getInstanceByDefinition",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_Schedule_getInstanceByDefinition(
            final IsolateThread thread,
            final CCharPointer scheduleDefinition,
            @DxfgOut final WordPointer schedule
    ) {
        if (schedule.isNull()) {
            throw new IllegalArgumentException("The `schedule` pointer is null");
        }

        final Throwable stored = OlderEntryPoint.takeStoredException();
        final DxfgScheduleHandle result = ScheduleNative.dxfg_Schedule_getInstance2(thread, scheduleDefinition);

        if (OlderEntryPoint.failed(stored, result.isNull())) {
            return ExceptionHandlerReturnMinusOne.EXECUTE_FAIL;
        }

        schedule.write(result);

        return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
    }

    @CEntryPoint(
            name = "dxfg_v4_Schedule_getInstanceByProfileHandleWithVenue",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_Schedule_getInstanceByProfileHandleWithVenue(
            final IsolateThread thread,
            final DxfgInstrumentProfile dxfgInstrumentProfile,
            final CCharPointer venue,
            @DxfgOut final WordPointer schedule
    ) {
        if (schedule.isNull()) {
            throw new IllegalArgumentException("The `schedule` pointer is null");
        }

        final Throwable stored = OlderEntryPoint.takeStoredException();
        final DxfgScheduleHandle result = ScheduleNative.dxfg_Schedule_getInstance3(thread, dxfgInstrumentProfile, venue);

        if (OlderEntryPoint.failed(stored, result.isNull())) {
            return ExceptionHandlerReturnMinusOne.EXECUTE_FAIL;
        }

        schedule.write(result);

        return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
    }

    @CEntryPoint(
            name = "dxfg_v4_Schedule_getTradingVenuesByProfileHandle",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_Schedule_getTradingVenuesByProfileHandle(
            final IsolateThread thread,
            final DxfgInstrumentProfile dxfgInstrumentProfile,
            @DxfgOut final WordPointer venues
    ) {
        if (venues.isNull()) {
            throw new IllegalArgumentException("The `venues` pointer is null");
        }

        final Throwable stored = OlderEntryPoint.takeStoredException();
        final DxfgCStringListPointer result = ScheduleNative.dxfg_Schedule_getTradingVenues(thread, dxfgInstrumentProfile);

        if (OlderEntryPoint.failed(stored, result.isNull())) {
            return ExceptionHandlerReturnMinusOne.EXECUTE_FAIL;
        }

        venues.write(result);

        return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
    }

    @CEntryPoint(
            name = "dxfg_v4_Schedule_downloadDefaults",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_Schedule_downloadDefaults(
            final IsolateThread thread,
            final CCharPointer scheduleDefinition
    ) {
        final Throwable stored = OlderEntryPoint.takeStoredException();
        final int result = ScheduleNative.dxfg_Schedule_downloadDefaults(thread, scheduleDefinition);

        return OlderEntryPoint.failed(stored, result == ExceptionHandlerReturnMinusOne.EXECUTE_FAIL)
                ? ExceptionHandlerReturnMinusOne.EXECUTE_FAIL
                : result;
    }

    @CEntryPoint(
            name = "dxfg_v4_Schedule_setDefaults",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_Schedule_setDefaults(
            final IsolateThread thread,
            final CCharPointer data,
            final int size
    ) throws IOException {
        final Throwable stored = OlderEntryPoint.takeStoredException();
        final int result = ScheduleNative.dxfg_Schedule_setDefaults(thread, data, size);

        return OlderEntryPoint.failed(stored, result == ExceptionHandlerReturnMinusOne.EXECUTE_FAIL)
                ? ExceptionHandlerReturnMinusOne.EXECUTE_FAIL
                : result;
    }

    @CEntryPoint(
            name = "dxfg_v4_Schedule_getSessionByTime",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_Schedule_getSessionByTime(
            final IsolateThread thread,
            final DxfgScheduleHandle dxfgSchedule,
            final long time,
            @DxfgOut final WordPointer session
    ) {
        if (session.isNull()) {
            throw new IllegalArgumentException("The `session` pointer is null");
        }

        final Throwable stored = OlderEntryPoint.takeStoredException();
        final DxfgSession result = ScheduleNative.dxfg_Schedule_getSessionByTime(thread, dxfgSchedule, time);

        if (OlderEntryPoint.failed(stored, result.isNull())) {
            return ExceptionHandlerReturnMinusOne.EXECUTE_FAIL;
        }

        session.write(result);

        return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
    }

    @CEntryPoint(
            name = "dxfg_v4_Schedule_getDayByTime",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_Schedule_getDayByTime(
            final IsolateThread thread,
            final DxfgScheduleHandle dxfgSchedule,
            final long time,
            @DxfgOut final WordPointer day
    ) {
        if (day.isNull()) {
            throw new IllegalArgumentException("The `day` pointer is null");
        }

        final Throwable stored = OlderEntryPoint.takeStoredException();
        final DxfgDay result = ScheduleNative.dxfg_Schedule_getDayByTime(thread, dxfgSchedule, time);

        if (OlderEntryPoint.failed(stored, result.isNull())) {
            return ExceptionHandlerReturnMinusOne.EXECUTE_FAIL;
        }

        day.write(result);

        return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
    }

    @CEntryPoint(
            name = "dxfg_v4_Schedule_getDayById",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_Schedule_getDayById(
            final IsolateThread thread,
            final DxfgScheduleHandle dxfgSchedule,
            final int dayId,
            @DxfgOut final WordPointer day
    ) {
        if (day.isNull()) {
            throw new IllegalArgumentException("The `day` pointer is null");
        }

        final Throwable stored = OlderEntryPoint.takeStoredException();
        final DxfgDay result = ScheduleNative.dxfg_Schedule_getDayById(thread, dxfgSchedule, dayId);

        if (OlderEntryPoint.failed(stored, result.isNull())) {
            return ExceptionHandlerReturnMinusOne.EXECUTE_FAIL;
        }

        day.write(result);

        return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
    }

    @CEntryPoint(
            name = "dxfg_v4_Schedule_getDayByYearMonthDay",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_Schedule_getDayByYearMonthDay(
            final IsolateThread thread,
            final DxfgScheduleHandle dxfgSchedule,
            final int yearMonthDay,
            @DxfgOut final WordPointer day
    ) {
        if (day.isNull()) {
            throw new IllegalArgumentException("The `day` pointer is null");
        }

        final Throwable stored = OlderEntryPoint.takeStoredException();
        final DxfgDay result = ScheduleNative.dxfg_Schedule_getDayByYearMonthDay(thread, dxfgSchedule, yearMonthDay);

        if (OlderEntryPoint.failed(stored, result.isNull())) {
            return ExceptionHandlerReturnMinusOne.EXECUTE_FAIL;
        }

        day.write(result);

        return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
    }

    @CEntryPoint(
            name = "dxfg_v4_Schedule_getNearestSessionByTime",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_Schedule_getNearestSessionByTime(
            final IsolateThread thread,
            final DxfgScheduleHandle dxfgSchedule,
            final long time,
            final DxfgSessionFilter dxfgSessionFilter,
            @DxfgOut final WordPointer session
    ) {
        if (session.isNull()) {
            throw new IllegalArgumentException("The `session` pointer is null");
        }

        final Throwable stored = OlderEntryPoint.takeStoredException();
        final DxfgSession result = ScheduleNative.dxfg_Schedule_getNearestSessionByTime(
                        thread,
                        dxfgSchedule,
                        time,
                        dxfgSessionFilter);

        if (OlderEntryPoint.failed(stored, result.isNull())) {
            return ExceptionHandlerReturnMinusOne.EXECUTE_FAIL;
        }

        session.write(result);

        return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
    }

    @CEntryPoint(
            name = "dxfg_v4_Schedule_findNearestSessionByTime",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_Schedule_findNearestSessionByTime(
            final IsolateThread thread,
            final DxfgScheduleHandle dxfgSchedule,
            final long time,
            final DxfgSessionFilter dxfgSessionFilter,
            @DxfgOut final WordPointer session
    ) {
        if (session.isNull()) {
            throw new IllegalArgumentException("The `session` pointer is null");
        }

        final Throwable stored = OlderEntryPoint.takeStoredException();
        final DxfgSession result = ScheduleNative.dxfg_Schedule_findNearestSessionByTime(
                        thread,
                        dxfgSchedule,
                        time,
                        dxfgSessionFilter);

        if (OlderEntryPoint.failed(stored, result.isNull())) {
            return ExceptionHandlerReturnMinusOne.EXECUTE_FAIL;
        }

        session.write(result);

        return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
    }

    @CEntryPoint(
            name = "dxfg_v4_Schedule_getName",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_Schedule_getName(
            final IsolateThread thread,
            final DxfgScheduleHandle dxfgSchedule,
            @DxfgOut final WordPointer name
    ) {
        if (name.isNull()) {
            throw new IllegalArgumentException("The `name` pointer is null");
        }

        final Throwable stored = OlderEntryPoint.takeStoredException();
        final CCharPointer result = ScheduleNative.dxfg_Schedule_getName(thread, dxfgSchedule);

        if (OlderEntryPoint.failed(stored, result.isNull())) {
            return ExceptionHandlerReturnMinusOne.EXECUTE_FAIL;
        }

        name.write(result);

        return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
    }

    @CEntryPoint(
            name = "dxfg_v4_Schedule_getTimeZone",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_Schedule_getTimeZone(
            final IsolateThread thread,
            final DxfgScheduleHandle dxfgSchedule,
            @DxfgOut final WordPointer timeZoneDisplayName
    ) {
        if (timeZoneDisplayName.isNull()) {
            throw new IllegalArgumentException("The `timeZoneDisplayName` pointer is null");
        }

        final Throwable stored = OlderEntryPoint.takeStoredException();
        final CCharPointer result = ScheduleNative.dxfg_Schedule_getTimeZone(thread, dxfgSchedule);

        if (OlderEntryPoint.failed(stored, result.isNull())) {
            return ExceptionHandlerReturnMinusOne.EXECUTE_FAIL;
        }

        timeZoneDisplayName.write(result);

        return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
    }

    @CEntryPoint(
            name = "dxfg_v4_Schedule_getTimeZone_getID",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_Schedule_getTimeZone_getID(
            final IsolateThread thread,
            final DxfgScheduleHandle dxfgSchedule,
            @DxfgOut final WordPointer timeZoneId
    ) {
        if (timeZoneId.isNull()) {
            throw new IllegalArgumentException("The `timeZoneId` pointer is null");
        }

        final Throwable stored = OlderEntryPoint.takeStoredException();
        final CCharPointer result = ScheduleNative.dxfg_Schedule_getTimeZone_getID(thread, dxfgSchedule);

        if (OlderEntryPoint.failed(stored, result.isNull())) {
            return ExceptionHandlerReturnMinusOne.EXECUTE_FAIL;
        }

        timeZoneId.write(result);

        return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
    }

    @CEntryPoint(
            name = "dxfg_v4_Schedule_getDefinition",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_Schedule_getDefinition(
            final IsolateThread thread,
            final DxfgScheduleHandle dxfgSchedule,
            @DxfgOut final CCharPointerPointer definition
    ) {
        final Throwable stored = OlderEntryPoint.takeStoredException();
        final int result = ScheduleNative.dxfg_Schedule_getDefinition(thread, dxfgSchedule, definition);

        return OlderEntryPoint.failed(stored, result == ExceptionHandlerReturnMinusOne.EXECUTE_FAIL)
                ? ExceptionHandlerReturnMinusOne.EXECUTE_FAIL
                : result;
    }

    @CEntryPoint(
            name = "dxfg_v4_Session_getDay",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_Session_getDay(
            final IsolateThread thread,
            final DxfgSession dxfgSession,
            @DxfgOut final WordPointer day
    ) {
        if (day.isNull()) {
            throw new IllegalArgumentException("The `day` pointer is null");
        }

        final Throwable stored = OlderEntryPoint.takeStoredException();
        final DxfgDay result = ScheduleNative.dxfg_Session_getDay(thread, dxfgSession);

        if (OlderEntryPoint.failed(stored, result.isNull())) {
            return ExceptionHandlerReturnMinusOne.EXECUTE_FAIL;
        }

        day.write(result);

        return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
    }

    @CEntryPoint(
            name = "dxfg_v4_Session_getType",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_Session_getType(
            final IsolateThread thread,
            final DxfgSession dxfgSession,
            @DxfgOut final CInt32Pointer type
    ) {
        if (type.isNull()) {
            throw new IllegalArgumentException("The `type` pointer is null");
        }

        final Throwable stored = OlderEntryPoint.takeStoredException();
        final int result = ScheduleNative.dxfg_Session_getType(thread, dxfgSession);

        if (OlderEntryPoint.failed(stored, result == ExceptionHandlerReturnMinusOne.EXECUTE_FAIL)) {
            return ExceptionHandlerReturnMinusOne.EXECUTE_FAIL;
        }

        type.write(result);

        return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
    }

    @CEntryPoint(
            name = "dxfg_v4_Session_isTrading",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_Session_isTrading(
            final IsolateThread thread,
            final DxfgSession dxfgSession,
            @DxfgOut final CInt32Pointer trading
    ) {
        if (trading.isNull()) {
            throw new IllegalArgumentException("The `trading` pointer is null");
        }

        final Throwable stored = OlderEntryPoint.takeStoredException();
        final int result = ScheduleNative.dxfg_Session_isTrading(thread, dxfgSession);

        if (OlderEntryPoint.failed(stored, result == ExceptionHandlerReturnMinusOne.EXECUTE_FAIL)) {
            return ExceptionHandlerReturnMinusOne.EXECUTE_FAIL;
        }

        trading.write(result);

        return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
    }

    @CEntryPoint(
            name = "dxfg_v4_Session_isEmpty",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_Session_isEmpty(
            final IsolateThread thread,
            final DxfgSession dxfgSession,
            @DxfgOut final CInt32Pointer empty
    ) {
        if (empty.isNull()) {
            throw new IllegalArgumentException("The `empty` pointer is null");
        }

        final Throwable stored = OlderEntryPoint.takeStoredException();
        final int result = ScheduleNative.dxfg_Session_isEmpty(thread, dxfgSession);

        if (OlderEntryPoint.failed(stored, result == ExceptionHandlerReturnMinusOne.EXECUTE_FAIL)) {
            return ExceptionHandlerReturnMinusOne.EXECUTE_FAIL;
        }

        empty.write(result);

        return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
    }

    @CEntryPoint(
            name = "dxfg_v4_Session_getStartTime",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_Session_getStartTime(
            final IsolateThread thread,
            final DxfgSession dxfgSession,
            @DxfgOut final CLongPointer startTime
    ) {
        if (startTime.isNull()) {
            throw new IllegalArgumentException("The `startTime` pointer is null");
        }

        final Throwable stored = OlderEntryPoint.takeStoredException();
        final long result = ScheduleNative.dxfg_Session_getStartTime(thread, dxfgSession);

        if (OlderEntryPoint.failed(stored, result == ExceptionHandlerReturnMinusOne.EXECUTE_FAIL)) {
            return ExceptionHandlerReturnMinusOne.EXECUTE_FAIL;
        }

        startTime.write(result);

        return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
    }

    @CEntryPoint(
            name = "dxfg_v4_Session_getEndTime",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_Session_getEndTime(
            final IsolateThread thread,
            final DxfgSession dxfgSession,
            @DxfgOut final CLongPointer endTime
    ) {
        if (endTime.isNull()) {
            throw new IllegalArgumentException("The `endTime` pointer is null");
        }

        final Throwable stored = OlderEntryPoint.takeStoredException();
        final long result = ScheduleNative.dxfg_Session_getEndTime(thread, dxfgSession);

        if (OlderEntryPoint.failed(stored, result == ExceptionHandlerReturnMinusOne.EXECUTE_FAIL)) {
            return ExceptionHandlerReturnMinusOne.EXECUTE_FAIL;
        }

        endTime.write(result);

        return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
    }

    @CEntryPoint(
            name = "dxfg_v4_Session_containsTime",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_Session_containsTime(
            final IsolateThread thread,
            final DxfgSession dxfgSession,
            final long time,
            @DxfgOut final CInt32Pointer contains
    ) {
        if (contains.isNull()) {
            throw new IllegalArgumentException("The `contains` pointer is null");
        }

        final Throwable stored = OlderEntryPoint.takeStoredException();
        final int result = ScheduleNative.dxfg_Session_containsTime(thread, dxfgSession, time);

        if (OlderEntryPoint.failed(stored, result == ExceptionHandlerReturnMinusOne.EXECUTE_FAIL)) {
            return ExceptionHandlerReturnMinusOne.EXECUTE_FAIL;
        }

        contains.write(result);

        return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
    }

    @CEntryPoint(
            name = "dxfg_v4_Session_getPrevSession",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_Session_getPrevSession(
            final IsolateThread thread,
            final DxfgSession dxfgSession,
            final DxfgSessionFilter dxfgSessionFilter,
            @DxfgOut final WordPointer prevSession
    ) {
        if (prevSession.isNull()) {
            throw new IllegalArgumentException("The `prevSession` pointer is null");
        }

        final Throwable stored = OlderEntryPoint.takeStoredException();
        final DxfgSession result = ScheduleNative.dxfg_Session_getPrevSession(thread, dxfgSession, dxfgSessionFilter);

        if (OlderEntryPoint.failed(stored, result.isNull())) {
            return ExceptionHandlerReturnMinusOne.EXECUTE_FAIL;
        }

        prevSession.write(result);

        return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
    }

    @CEntryPoint(
            name = "dxfg_v4_Session_getNextSession",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_Session_getNextSession(
            final IsolateThread thread,
            final DxfgSession dxfgSession,
            final DxfgSessionFilter dxfgSessionFilter,
            @DxfgOut final WordPointer nextSession
    ) {
        if (nextSession.isNull()) {
            throw new IllegalArgumentException("The `nextSession` pointer is null");
        }

        final Throwable stored = OlderEntryPoint.takeStoredException();
        final DxfgSession result = ScheduleNative.dxfg_Session_getNextSession(thread, dxfgSession, dxfgSessionFilter);

        if (OlderEntryPoint.failed(stored, result.isNull())) {
            return ExceptionHandlerReturnMinusOne.EXECUTE_FAIL;
        }

        nextSession.write(result);

        return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
    }

    @CEntryPoint(
            name = "dxfg_v4_Session_findPrevSession",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_Session_findPrevSession(
            final IsolateThread thread,
            final DxfgSession dxfgSession,
            final DxfgSessionFilter dxfgSessionFilter,
            @DxfgOut final WordPointer prevSession
    ) {
        if (prevSession.isNull()) {
            throw new IllegalArgumentException("The `prevSession` pointer is null");
        }

        final Throwable stored = OlderEntryPoint.takeStoredException();
        final DxfgSession result = ScheduleNative.dxfg_Session_findPrevSession(thread, dxfgSession, dxfgSessionFilter);

        if (OlderEntryPoint.failed(stored, result.isNull())) {
            return ExceptionHandlerReturnMinusOne.EXECUTE_FAIL;
        }

        prevSession.write(result);

        return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
    }

    @CEntryPoint(
            name = "dxfg_v4_Session_findNextSession",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_Session_findNextSession(
            final IsolateThread thread,
            final DxfgSession dxfgSession,
            final DxfgSessionFilter dxfgSessionFilter,
            @DxfgOut final WordPointer nextSession
    ) {
        if (nextSession.isNull()) {
            throw new IllegalArgumentException("The `nextSession` pointer is null");
        }

        final Throwable stored = OlderEntryPoint.takeStoredException();
        final DxfgSession result = ScheduleNative.dxfg_Session_findNextSession(thread, dxfgSession, dxfgSessionFilter);

        if (OlderEntryPoint.failed(stored, result.isNull())) {
            return ExceptionHandlerReturnMinusOne.EXECUTE_FAIL;
        }

        nextSession.write(result);

        return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
    }

    @CEntryPoint(
            name = "dxfg_v4_Session_hashCode",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_Session_hashCode(
            final IsolateThread thread,
            final DxfgSession dxfgSession,
            @DxfgOut final CInt32Pointer hashCode
    ) {
        if (hashCode.isNull()) {
            throw new IllegalArgumentException("The `hashCode` pointer is null");
        }

        final Throwable stored = OlderEntryPoint.takeStoredException();
        final int result = ScheduleNative.dxfg_Session_hashCode(thread, dxfgSession);

        if (OlderEntryPoint.failed(stored, result == ExceptionHandlerReturnMinusOne.EXECUTE_FAIL)) {
            return ExceptionHandlerReturnMinusOne.EXECUTE_FAIL;
        }

        hashCode.write(result);

        return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
    }

    @CEntryPoint(
            name = "dxfg_v4_Session_equals",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_Session_equals(
            final IsolateThread thread,
            final DxfgSession dxfgSession,
            final DxfgSession dxfgOtherSession,
            @DxfgOut final CInt32Pointer equal
    ) {
        if (equal.isNull()) {
            throw new IllegalArgumentException("The `equal` pointer is null");
        }

        final Throwable stored = OlderEntryPoint.takeStoredException();
        final int result = ScheduleNative.dxfg_Session_equals(thread, dxfgSession, dxfgOtherSession);

        if (OlderEntryPoint.failed(stored, result == ExceptionHandlerReturnMinusOne.EXECUTE_FAIL)) {
            return ExceptionHandlerReturnMinusOne.EXECUTE_FAIL;
        }

        equal.write(result);

        return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
    }

    @CEntryPoint(
            name = "dxfg_v4_Session_toString",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_Session_toString(
            final IsolateThread thread,
            final DxfgSession dxfgSession,
            @DxfgOut final WordPointer string
    ) {
        if (string.isNull()) {
            throw new IllegalArgumentException("The `string` pointer is null");
        }

        final Throwable stored = OlderEntryPoint.takeStoredException();
        final CCharPointer result = ScheduleNative.dxfg_Session_toString(thread, dxfgSession);

        if (OlderEntryPoint.failed(stored, result.isNull())) {
            return ExceptionHandlerReturnMinusOne.EXECUTE_FAIL;
        }

        string.write(result);

        return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
    }

    @CEntryPoint(
            name = "dxfg_v4_Day_getSchedule",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_Day_getSchedule(
            final IsolateThread thread,
            final DxfgDay dxfgDay,
            @DxfgOut final WordPointer schedule
    ) {
        if (schedule.isNull()) {
            throw new IllegalArgumentException("The `schedule` pointer is null");
        }

        final Throwable stored = OlderEntryPoint.takeStoredException();
        final DxfgScheduleHandle result = ScheduleNative.dxfg_Day_getSchedule(thread, dxfgDay);

        if (OlderEntryPoint.failed(stored, result.isNull())) {
            return ExceptionHandlerReturnMinusOne.EXECUTE_FAIL;
        }

        schedule.write(result);

        return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
    }

    @CEntryPoint(
            name = "dxfg_v4_Day_getDayId",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_Day_getDayId(
            final IsolateThread thread,
            final DxfgDay dxfgDay,
            @DxfgOut final CInt32Pointer dayId
    ) {
        if (dayId.isNull()) {
            throw new IllegalArgumentException("The `dayId` pointer is null");
        }

        final Throwable stored = OlderEntryPoint.takeStoredException();
        final int result = ScheduleNative.dxfg_Day_getDayId(thread, dxfgDay);

        if (OlderEntryPoint.failed(stored, result == ExceptionHandlerReturnMinusOne.EXECUTE_FAIL)) {
            return ExceptionHandlerReturnMinusOne.EXECUTE_FAIL;
        }

        dayId.write(result);

        return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
    }

    @CEntryPoint(
            name = "dxfg_v4_Day_getYearMonthDay",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_Day_getYearMonthDay(
            final IsolateThread thread,
            final DxfgDay dxfgDay,
            @DxfgOut final CInt32Pointer yearMonthDay
    ) {
        if (yearMonthDay.isNull()) {
            throw new IllegalArgumentException("The `yearMonthDay` pointer is null");
        }

        final Throwable stored = OlderEntryPoint.takeStoredException();
        final int result = ScheduleNative.dxfg_Day_getYearMonthDay(thread, dxfgDay);

        if (OlderEntryPoint.failed(stored, result == ExceptionHandlerReturnMinusOne.EXECUTE_FAIL)) {
            return ExceptionHandlerReturnMinusOne.EXECUTE_FAIL;
        }

        yearMonthDay.write(result);

        return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
    }

    @CEntryPoint(
            name = "dxfg_v4_Day_getYear",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_Day_getYear(
            final IsolateThread thread,
            final DxfgDay dxfgDay,
            @DxfgOut final CInt32Pointer year
    ) {
        if (year.isNull()) {
            throw new IllegalArgumentException("The `year` pointer is null");
        }

        final Throwable stored = OlderEntryPoint.takeStoredException();
        final int result = ScheduleNative.dxfg_Day_getYear(thread, dxfgDay);

        if (OlderEntryPoint.failed(stored, result == ExceptionHandlerReturnMinusOne.EXECUTE_FAIL)) {
            return ExceptionHandlerReturnMinusOne.EXECUTE_FAIL;
        }

        year.write(result);

        return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
    }

    @CEntryPoint(
            name = "dxfg_v4_Day_getMonthOfYear",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_Day_getMonthOfYear(
            final IsolateThread thread,
            final DxfgDay dxfgDay,
            @DxfgOut final CInt32Pointer monthOfYear
    ) {
        if (monthOfYear.isNull()) {
            throw new IllegalArgumentException("The `monthOfYear` pointer is null");
        }

        final Throwable stored = OlderEntryPoint.takeStoredException();
        final int result = ScheduleNative.dxfg_Day_getMonthOfYear(thread, dxfgDay);

        if (OlderEntryPoint.failed(stored, result == ExceptionHandlerReturnMinusOne.EXECUTE_FAIL)) {
            return ExceptionHandlerReturnMinusOne.EXECUTE_FAIL;
        }

        monthOfYear.write(result);

        return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
    }

    @CEntryPoint(
            name = "dxfg_v4_Day_getDayOfMonth",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_Day_getDayOfMonth(
            final IsolateThread thread,
            final DxfgDay dxfgDay,
            @DxfgOut final CInt32Pointer dayOfMonth
    ) {
        if (dayOfMonth.isNull()) {
            throw new IllegalArgumentException("The `dayOfMonth` pointer is null");
        }

        final Throwable stored = OlderEntryPoint.takeStoredException();
        final int result = ScheduleNative.dxfg_Day_getDayOfMonth(thread, dxfgDay);

        if (OlderEntryPoint.failed(stored, result == ExceptionHandlerReturnMinusOne.EXECUTE_FAIL)) {
            return ExceptionHandlerReturnMinusOne.EXECUTE_FAIL;
        }

        dayOfMonth.write(result);

        return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
    }

    @CEntryPoint(
            name = "dxfg_v4_Day_getDayOfWeek",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_Day_getDayOfWeek(
            final IsolateThread thread,
            final DxfgDay dxfgDay,
            @DxfgOut final CInt32Pointer dayOfWeek
    ) {
        if (dayOfWeek.isNull()) {
            throw new IllegalArgumentException("The `dayOfWeek` pointer is null");
        }

        final Throwable stored = OlderEntryPoint.takeStoredException();
        final int result = ScheduleNative.dxfg_Day_getDayOfWeek(thread, dxfgDay);

        if (OlderEntryPoint.failed(stored, result == ExceptionHandlerReturnMinusOne.EXECUTE_FAIL)) {
            return ExceptionHandlerReturnMinusOne.EXECUTE_FAIL;
        }

        dayOfWeek.write(result);

        return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
    }

    @CEntryPoint(
            name = "dxfg_v4_Day_isHoliday",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_Day_isHoliday(
            final IsolateThread thread,
            final DxfgDay dxfgDay,
            @DxfgOut final CInt32Pointer holiday
    ) {
        if (holiday.isNull()) {
            throw new IllegalArgumentException("The `holiday` pointer is null");
        }

        final Throwable stored = OlderEntryPoint.takeStoredException();
        final int result = ScheduleNative.dxfg_Day_isHoliday(thread, dxfgDay);

        if (OlderEntryPoint.failed(stored, result == ExceptionHandlerReturnMinusOne.EXECUTE_FAIL)) {
            return ExceptionHandlerReturnMinusOne.EXECUTE_FAIL;
        }

        holiday.write(result);

        return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
    }

    @CEntryPoint(
            name = "dxfg_v4_Day_isShortDay",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_Day_isShortDay(
            final IsolateThread thread,
            final DxfgDay dxfgDay,
            @DxfgOut final CInt32Pointer shortDay
    ) {
        if (shortDay.isNull()) {
            throw new IllegalArgumentException("The `shortDay` pointer is null");
        }

        final Throwable stored = OlderEntryPoint.takeStoredException();
        final int result = ScheduleNative.dxfg_Day_isShortDay(thread, dxfgDay);

        if (OlderEntryPoint.failed(stored, result == ExceptionHandlerReturnMinusOne.EXECUTE_FAIL)) {
            return ExceptionHandlerReturnMinusOne.EXECUTE_FAIL;
        }

        shortDay.write(result);

        return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
    }

    @CEntryPoint(
            name = "dxfg_v4_Day_isTrading",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_Day_isTrading(
            final IsolateThread thread,
            final DxfgDay dxfgDay,
            @DxfgOut final CInt32Pointer trading
    ) {
        if (trading.isNull()) {
            throw new IllegalArgumentException("The `trading` pointer is null");
        }

        final Throwable stored = OlderEntryPoint.takeStoredException();
        final int result = ScheduleNative.dxfg_Day_isTrading(thread, dxfgDay);

        if (OlderEntryPoint.failed(stored, result == ExceptionHandlerReturnMinusOne.EXECUTE_FAIL)) {
            return ExceptionHandlerReturnMinusOne.EXECUTE_FAIL;
        }

        trading.write(result);

        return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
    }

    @CEntryPoint(
            name = "dxfg_v4_Day_getStartTime",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_Day_getStartTime(
            final IsolateThread thread,
            final DxfgDay dxfgDay,
            @DxfgOut final CLongPointer startTime
    ) {
        if (startTime.isNull()) {
            throw new IllegalArgumentException("The `startTime` pointer is null");
        }

        final Throwable stored = OlderEntryPoint.takeStoredException();
        final long result = ScheduleNative.dxfg_Day_getStartTime(thread, dxfgDay);

        if (OlderEntryPoint.failed(stored, result == ExceptionHandlerReturnMinusOne.EXECUTE_FAIL)) {
            return ExceptionHandlerReturnMinusOne.EXECUTE_FAIL;
        }

        startTime.write(result);

        return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
    }

    @CEntryPoint(
            name = "dxfg_v4_Day_getEndTime",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_Day_getEndTime(
            final IsolateThread thread,
            final DxfgDay dxfgDay,
            @DxfgOut final CLongPointer endTime
    ) {
        if (endTime.isNull()) {
            throw new IllegalArgumentException("The `endTime` pointer is null");
        }

        final Throwable stored = OlderEntryPoint.takeStoredException();
        final long result = ScheduleNative.dxfg_Day_getEndTime(thread, dxfgDay);

        if (OlderEntryPoint.failed(stored, result == ExceptionHandlerReturnMinusOne.EXECUTE_FAIL)) {
            return ExceptionHandlerReturnMinusOne.EXECUTE_FAIL;
        }

        endTime.write(result);

        return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
    }

    @CEntryPoint(
            name = "dxfg_v4_Day_containsTime",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_Day_containsTime(
            final IsolateThread thread,
            final DxfgDay dxfgDay,
            final long time,
            @DxfgOut final CInt32Pointer contains
    ) {
        if (contains.isNull()) {
            throw new IllegalArgumentException("The `contains` pointer is null");
        }

        final Throwable stored = OlderEntryPoint.takeStoredException();
        final int result = ScheduleNative.dxfg_Day_containsTime(thread, dxfgDay, time);

        if (OlderEntryPoint.failed(stored, result == ExceptionHandlerReturnMinusOne.EXECUTE_FAIL)) {
            return ExceptionHandlerReturnMinusOne.EXECUTE_FAIL;
        }

        contains.write(result);

        return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
    }

    @CEntryPoint(
            name = "dxfg_v4_Day_getResetTime",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_Day_getResetTime(
            final IsolateThread thread,
            final DxfgDay dxfgDay,
            @DxfgOut final CLongPointer resetTime
    ) {
        if (resetTime.isNull()) {
            throw new IllegalArgumentException("The `resetTime` pointer is null");
        }

        final Throwable stored = OlderEntryPoint.takeStoredException();
        final long result = ScheduleNative.dxfg_Day_getResetTime(thread, dxfgDay);

        if (OlderEntryPoint.failed(stored, result == ExceptionHandlerReturnMinusOne.EXECUTE_FAIL)) {
            return ExceptionHandlerReturnMinusOne.EXECUTE_FAIL;
        }

        resetTime.write(result);

        return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
    }

    @CEntryPoint(
            name = "dxfg_v4_Day_getSessions",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_Day_getSessions(
            final IsolateThread thread,
            final DxfgDay dxfgDay,
            @DxfgOut final WordPointer sessions
    ) {
        if (sessions.isNull()) {
            throw new IllegalArgumentException("The `sessions` pointer is null");
        }

        final Throwable stored = OlderEntryPoint.takeStoredException();
        final DxfgSessionList result = ScheduleNative.dxfg_Day_getSessions(thread, dxfgDay);

        if (OlderEntryPoint.failed(stored, result.isNull())) {
            return ExceptionHandlerReturnMinusOne.EXECUTE_FAIL;
        }

        sessions.write(result);

        return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
    }

    @CEntryPoint(
            name = "dxfg_v4_Day_getSessionByTime",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_Day_getSessionByTime(
            final IsolateThread thread,
            final DxfgDay dxfgDay,
            final long time,
            @DxfgOut final WordPointer session
    ) {
        if (session.isNull()) {
            throw new IllegalArgumentException("The `session` pointer is null");
        }

        final Throwable stored = OlderEntryPoint.takeStoredException();
        final DxfgSession result = ScheduleNative.dxfg_Day_getSessionByTime(thread, dxfgDay, time);

        if (OlderEntryPoint.failed(stored, result.isNull())) {
            return ExceptionHandlerReturnMinusOne.EXECUTE_FAIL;
        }

        session.write(result);

        return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
    }

    @CEntryPoint(
            name = "dxfg_v4_Day_getFirstSession",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_Day_getFirstSession(
            final IsolateThread thread,
            final DxfgDay dxfgDay,
            final DxfgSessionFilter dxfgSessionFilter,
            @DxfgOut final WordPointer session
    ) {
        if (session.isNull()) {
            throw new IllegalArgumentException("The `session` pointer is null");
        }

        final Throwable stored = OlderEntryPoint.takeStoredException();
        final DxfgSession result = ScheduleNative.dxfg_Day_getFirstSession(thread, dxfgDay, dxfgSessionFilter);

        if (OlderEntryPoint.failed(stored, result.isNull())) {
            return ExceptionHandlerReturnMinusOne.EXECUTE_FAIL;
        }

        session.write(result);

        return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
    }

    @CEntryPoint(
            name = "dxfg_v4_Day_getLastSession",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_Day_getLastSession(
            final IsolateThread thread,
            final DxfgDay dxfgDay,
            final DxfgSessionFilter dxfgSessionFilter,
            @DxfgOut final WordPointer session
    ) {
        if (session.isNull()) {
            throw new IllegalArgumentException("The `session` pointer is null");
        }

        final Throwable stored = OlderEntryPoint.takeStoredException();
        final DxfgSession result = ScheduleNative.dxfg_Day_getLastSession(thread, dxfgDay, dxfgSessionFilter);

        if (OlderEntryPoint.failed(stored, result.isNull())) {
            return ExceptionHandlerReturnMinusOne.EXECUTE_FAIL;
        }

        session.write(result);

        return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
    }

    @CEntryPoint(
            name = "dxfg_v4_Day_findFirstSession",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_Day_findFirstSession(
            final IsolateThread thread,
            final DxfgDay dxfgDay,
            final DxfgSessionFilter dxfgSessionFilter,
            @DxfgOut final WordPointer session
    ) {
        if (session.isNull()) {
            throw new IllegalArgumentException("The `session` pointer is null");
        }

        final Throwable stored = OlderEntryPoint.takeStoredException();
        final DxfgSession result = ScheduleNative.dxfg_Day_findFirstSession(thread, dxfgDay, dxfgSessionFilter);

        if (OlderEntryPoint.failed(stored, result.isNull())) {
            return ExceptionHandlerReturnMinusOne.EXECUTE_FAIL;
        }

        session.write(result);

        return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
    }

    @CEntryPoint(
            name = "dxfg_v4_Day_findLastSession",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_Day_findLastSession(
            final IsolateThread thread,
            final DxfgDay dxfgDay,
            final DxfgSessionFilter dxfgSessionFilter,
            @DxfgOut final WordPointer session
    ) {
        if (session.isNull()) {
            throw new IllegalArgumentException("The `session` pointer is null");
        }

        final Throwable stored = OlderEntryPoint.takeStoredException();
        final DxfgSession result = ScheduleNative.dxfg_Day_findLastSession(thread, dxfgDay, dxfgSessionFilter);

        if (OlderEntryPoint.failed(stored, result.isNull())) {
            return ExceptionHandlerReturnMinusOne.EXECUTE_FAIL;
        }

        session.write(result);

        return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
    }

    @CEntryPoint(
            name = "dxfg_v4_Day_getPrevDay",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_Day_getPrevDay(
            final IsolateThread thread,
            final DxfgDay dxfgDay,
            final DxfgDayFilter dxfgDayFilter,
            @DxfgOut final WordPointer prevDay
    ) {
        if (prevDay.isNull()) {
            throw new IllegalArgumentException("The `prevDay` pointer is null");
        }

        final Throwable stored = OlderEntryPoint.takeStoredException();
        final DxfgDay result = ScheduleNative.dxfg_Day_getPrevDay(thread, dxfgDay, dxfgDayFilter);

        if (OlderEntryPoint.failed(stored, result.isNull())) {
            return ExceptionHandlerReturnMinusOne.EXECUTE_FAIL;
        }

        prevDay.write(result);

        return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
    }

    @CEntryPoint(
            name = "dxfg_v4_Day_getNextDay",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_Day_getNextDay(
            final IsolateThread thread,
            final DxfgDay dxfgDay,
            final DxfgDayFilter dxfgDayFilter,
            @DxfgOut final WordPointer nextDay
    ) {
        if (nextDay.isNull()) {
            throw new IllegalArgumentException("The `nextDay` pointer is null");
        }

        final Throwable stored = OlderEntryPoint.takeStoredException();
        final DxfgDay result = ScheduleNative.dxfg_Day_getNextDay(thread, dxfgDay, dxfgDayFilter);

        if (OlderEntryPoint.failed(stored, result.isNull())) {
            return ExceptionHandlerReturnMinusOne.EXECUTE_FAIL;
        }

        nextDay.write(result);

        return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
    }

    @CEntryPoint(
            name = "dxfg_v4_Day_findPrevDay",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_Day_findPrevDay(
            final IsolateThread thread,
            final DxfgDay dxfgDay,
            final DxfgDayFilter dxfgDayFilter,
            @DxfgOut final WordPointer prevDay
    ) {
        if (prevDay.isNull()) {
            throw new IllegalArgumentException("The `prevDay` pointer is null");
        }

        final Throwable stored = OlderEntryPoint.takeStoredException();
        final DxfgDay result = ScheduleNative.dxfg_Day_findPrevDay(thread, dxfgDay, dxfgDayFilter);

        if (OlderEntryPoint.failed(stored, result.isNull())) {
            return ExceptionHandlerReturnMinusOne.EXECUTE_FAIL;
        }

        prevDay.write(result);

        return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
    }

    @CEntryPoint(
            name = "dxfg_v4_Day_findNextDay",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_Day_findNextDay(
            final IsolateThread thread,
            final DxfgDay dxfgDay,
            final DxfgDayFilter dxfgDayFilter,
            @DxfgOut final WordPointer nextDay
    ) {
        if (nextDay.isNull()) {
            throw new IllegalArgumentException("The `nextDay` pointer is null");
        }

        final Throwable stored = OlderEntryPoint.takeStoredException();
        final DxfgDay result = ScheduleNative.dxfg_Day_findNextDay(thread, dxfgDay, dxfgDayFilter);

        if (OlderEntryPoint.failed(stored, result.isNull())) {
            return ExceptionHandlerReturnMinusOne.EXECUTE_FAIL;
        }

        nextDay.write(result);

        return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
    }

    @CEntryPoint(
            name = "dxfg_v4_Day_hashCode",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_Day_hashCode(
            final IsolateThread thread,
            final DxfgDay dxfgDay,
            @DxfgOut final CInt32Pointer hashCode
    ) {
        if (hashCode.isNull()) {
            throw new IllegalArgumentException("The `hashCode` pointer is null");
        }

        final Throwable stored = OlderEntryPoint.takeStoredException();
        final int result = ScheduleNative.dxfg_Day_hashCode(thread, dxfgDay);

        if (OlderEntryPoint.failed(stored, result == ExceptionHandlerReturnMinusOne.EXECUTE_FAIL)) {
            return ExceptionHandlerReturnMinusOne.EXECUTE_FAIL;
        }

        hashCode.write(result);

        return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
    }

    @CEntryPoint(
            name = "dxfg_v4_Day_equals",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_Day_equals(
            final IsolateThread thread,
            final DxfgDay dxfgDay,
            final DxfgDay dxfgOtherDay,
            @DxfgOut final CInt32Pointer equal
    ) {
        if (equal.isNull()) {
            throw new IllegalArgumentException("The `equal` pointer is null");
        }

        final Throwable stored = OlderEntryPoint.takeStoredException();
        final int result = ScheduleNative.dxfg_Day_equals(thread, dxfgDay, dxfgOtherDay);

        if (OlderEntryPoint.failed(stored, result == ExceptionHandlerReturnMinusOne.EXECUTE_FAIL)) {
            return ExceptionHandlerReturnMinusOne.EXECUTE_FAIL;
        }

        equal.write(result);

        return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
    }

    @CEntryPoint(
            name = "dxfg_v4_Day_toString",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_Day_toString(
            final IsolateThread thread,
            final DxfgDay dxfgDay,
            @DxfgOut final WordPointer string
    ) {
        if (string.isNull()) {
            throw new IllegalArgumentException("The `string` pointer is null");
        }

        final Throwable stored = OlderEntryPoint.takeStoredException();
        final CCharPointer result = ScheduleNative.dxfg_Day_toString(thread, dxfgDay);

        if (OlderEntryPoint.failed(stored, result.isNull())) {
            return ExceptionHandlerReturnMinusOne.EXECUTE_FAIL;
        }

        string.write(result);

        return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
    }

    @CEntryPoint(
            name = "dxfg_v4_Day_release",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_Day_release(
            final IsolateThread thread,
            final DxfgDay dxfgDay
    ) {
        final Throwable stored = OlderEntryPoint.takeStoredException();
        final int result = ScheduleNative.dxfg_Day_release(thread, dxfgDay);

        return OlderEntryPoint.failed(stored, result == ExceptionHandlerReturnMinusOne.EXECUTE_FAIL)
                ? ExceptionHandlerReturnMinusOne.EXECUTE_FAIL
                : result;
    }

    @CEntryPoint(
            name = "dxfg_v4_Session_release",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_Session_release(
            final IsolateThread thread,
            final DxfgSession dxfgSession
    ) {
        final Throwable stored = OlderEntryPoint.takeStoredException();
        final int result = ScheduleNative.dxfg_Session_release(thread, dxfgSession);

        return OlderEntryPoint.failed(stored, result == ExceptionHandlerReturnMinusOne.EXECUTE_FAIL)
                ? ExceptionHandlerReturnMinusOne.EXECUTE_FAIL
                : result;
    }

    @CEntryPoint(
            name = "dxfg_v4_session_list_wrapper_release",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_session_list_wrapper_release(
            final IsolateThread thread,
            final DxfgSessionList dxfgSessions
    ) {
        final Throwable stored = OlderEntryPoint.takeStoredException();
        final int result = ScheduleNative.dxfg_SessionList_wrapper_release(thread, dxfgSessions);

        return OlderEntryPoint.failed(stored, result == ExceptionHandlerReturnMinusOne.EXECUTE_FAIL)
                ? ExceptionHandlerReturnMinusOne.EXECUTE_FAIL
                : result;
    }

    @CEntryPoint(
            name = "dxfg_v4_Schedule_getInstance",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_Schedule_getInstance(
            final IsolateThread thread,
            final DxfgInstrumentProfile2Pointer instrumentProfile,
            @DxfgOut final DxfgScheduleHandlePointer schedule
    ) {
        final Throwable stored = OlderEntryPoint.takeStoredException();
        final int result = ScheduleNative.dxfg_Schedule_getInstance4(thread, instrumentProfile, schedule);

        return OlderEntryPoint.failed(stored, result == ExceptionHandlerReturnMinusOne.EXECUTE_FAIL)
                ? ExceptionHandlerReturnMinusOne.EXECUTE_FAIL
                : result;
    }

    @CEntryPoint(
            name = "dxfg_v4_Schedule_getInstanceWithVenue",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_Schedule_getInstanceWithVenue(
            final IsolateThread thread,
            final DxfgInstrumentProfile2Pointer instrumentProfile,
            @CConst final CCharPointer venue,
            @DxfgOut final DxfgScheduleHandlePointer schedule
    ) {
        final Throwable stored = OlderEntryPoint.takeStoredException();
        final int result = ScheduleNative.dxfg_Schedule_getInstance5(thread, instrumentProfile, venue, schedule);

        return OlderEntryPoint.failed(stored, result == ExceptionHandlerReturnMinusOne.EXECUTE_FAIL)
                ? ExceptionHandlerReturnMinusOne.EXECUTE_FAIL
                : result;
    }

    @CEntryPoint(
            name = "dxfg_v4_Schedule_getTradingVenues",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_Schedule_getTradingVenues(
            final IsolateThread thread,
            final DxfgInstrumentProfile2Pointer instrumentProfile,
            @DxfgOut final DxfgCStringListPointerPointer venues
    ) {
        final Throwable stored = OlderEntryPoint.takeStoredException();
        final int result = ScheduleNative.dxfg_Schedule_getTradingVenues2(thread, instrumentProfile, venues);

        return OlderEntryPoint.failed(stored, result == ExceptionHandlerReturnMinusOne.EXECUTE_FAIL)
                ? ExceptionHandlerReturnMinusOne.EXECUTE_FAIL
                : result;
    }

    @CEntryPoint(
            name = "dxfg_v4_session_list_release",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_session_list_release(
            final IsolateThread thread,
            final DxfgSessionList sessions
    ) {
        NativeUtils.MAPPER_SESSIONS.release(sessions);

        return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
    }
}
