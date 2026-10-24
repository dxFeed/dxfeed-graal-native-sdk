// Copyright (c) 2026 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

package com.dxfeed.sdk.feed;

import com.dxfeed.api.DXFeedSubscription;
import com.dxfeed.event.EventType;
import com.dxfeed.sdk.common.DxfgOut;
import com.dxfeed.sdk.events.DxfgEventClazz;
import com.dxfeed.sdk.events.DxfgEventClazzList;
import com.dxfeed.sdk.events.DxfgEventType;
import com.dxfeed.sdk.events.DxfgEventTypeListPointer;
import com.dxfeed.sdk.events.DxfgEventTypeListPointerPointer;
import com.dxfeed.sdk.events.DxfgEventTypePointer;
import com.dxfeed.sdk.exception.ExceptionHandlerReturnMinusOne;
import com.dxfeed.sdk.exception.OlderEntryPoint;
import com.dxfeed.sdk.subscription.DxfgSubscription;
import com.dxfeed.sdk.subscription.DxfgTimeSeriesSubscription;
import com.dxfeed.sdk.symbol.DxfgSymbol;
import org.graalvm.nativeimage.IsolateThread;
import org.graalvm.nativeimage.c.CContext;
import org.graalvm.nativeimage.c.function.CEntryPoint;
import org.graalvm.nativeimage.c.type.CCharPointer;
import org.graalvm.nativeimage.c.type.CConst;
import org.graalvm.nativeimage.c.type.WordPointer;

/**
 * The v4 twins of the functions of {@link FeedNative}: each one calls the old function as a Java method
 * ({@link OlderEntryPoint} tells whether it failed) and returns its result through an out parameter (see
 * dxfg_v4_api.h).
 */
@CContext(FeedDirectives.class)
public final class FeedV4Native {

    private FeedV4Native() {
    }

    @CEntryPoint(
            name = "dxfg_v4_DXFeed_getInstance",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_DXFeed_getInstance(
            final IsolateThread thread,
            @DxfgOut final WordPointer feed
    ) {
        if (feed.isNull()) {
            throw new IllegalArgumentException("The `feed` pointer is null");
        }

        final Throwable stored = OlderEntryPoint.takeStoredException();
        final DxfgFeed result = FeedNative.dxfg_DXFeed_getInstance(thread);

        if (OlderEntryPoint.failed(stored, result.isNull())) {
            return ExceptionHandlerReturnMinusOne.EXECUTE_FAIL;
        }

        feed.write(result);

        return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
    }

    @CEntryPoint(
            name = "dxfg_v4_DXFeed_createSubscription",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_DXFeed_createSubscription(
            final IsolateThread thread,
            final DxfgFeed dxfgFeed,
            final DxfgEventClazz dxfgClazz,
            @DxfgOut final WordPointer subscription
    ) {
        if (subscription.isNull()) {
            throw new IllegalArgumentException("The `subscription` pointer is null");
        }

        final Throwable stored = OlderEntryPoint.takeStoredException();
        final DxfgSubscription<DXFeedSubscription<EventType<?>>> result = FeedNative.dxfg_DXFeed_createSubscription(thread, dxfgFeed, dxfgClazz);

        if (OlderEntryPoint.failed(stored, result.isNull())) {
            return ExceptionHandlerReturnMinusOne.EXECUTE_FAIL;
        }

        subscription.write(result);

        return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
    }

    @CEntryPoint(
            name = "dxfg_v4_DXFeed_createSubscriptionWithEventTypes",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_DXFeed_createSubscriptionWithEventTypes(
            final IsolateThread thread,
            final DxfgFeed dxfgFeed,
            final DxfgEventClazzList eventClazzList,
            @DxfgOut final WordPointer subscription
    ) {
        if (subscription.isNull()) {
            throw new IllegalArgumentException("The `subscription` pointer is null");
        }

        final Throwable stored = OlderEntryPoint.takeStoredException();
        final DxfgSubscription<DXFeedSubscription<EventType<?>>> result = FeedNative.dxfg_DXFeed_createSubscription2(thread, dxfgFeed, eventClazzList);

        if (OlderEntryPoint.failed(stored, result.isNull())) {
            return ExceptionHandlerReturnMinusOne.EXECUTE_FAIL;
        }

        subscription.write(result);

        return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
    }

    @CEntryPoint(
            name = "dxfg_v4_DXFeed_createTimeSeriesSubscription",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_DXFeed_createTimeSeriesSubscription(
            final IsolateThread thread,
            final DxfgFeed feed,
            final DxfgEventClazz dxfgClazz,
            @DxfgOut final WordPointer timeSeriesSubscription
    ) {
        if (timeSeriesSubscription.isNull()) {
            throw new IllegalArgumentException("The `timeSeriesSubscription` pointer is null");
        }

        final Throwable stored = OlderEntryPoint.takeStoredException();
        final DxfgTimeSeriesSubscription result = FeedNative.dxfg_DXFeed_createTimeSeriesSubscription(thread, feed, dxfgClazz);

        if (OlderEntryPoint.failed(stored, result.isNull())) {
            return ExceptionHandlerReturnMinusOne.EXECUTE_FAIL;
        }

        timeSeriesSubscription.write(result);

        return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
    }

    @CEntryPoint(
            name = "dxfg_v4_DXFeed_createTimeSeriesSubscriptionWithEventTypes",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_DXFeed_createTimeSeriesSubscriptionWithEventTypes(
            final IsolateThread thread,
            final DxfgFeed feed,
            final DxfgEventClazzList eventClazzList,
            @DxfgOut final WordPointer timeSeriesSubscription
    ) {
        if (timeSeriesSubscription.isNull()) {
            throw new IllegalArgumentException("The `timeSeriesSubscription` pointer is null");
        }

        final Throwable stored = OlderEntryPoint.takeStoredException();
        final DxfgTimeSeriesSubscription result = FeedNative.dxfg_DXFeed_createTimeSeriesSubscription2(thread, feed, eventClazzList);

        if (OlderEntryPoint.failed(stored, result.isNull())) {
            return ExceptionHandlerReturnMinusOne.EXECUTE_FAIL;
        }

        timeSeriesSubscription.write(result);

        return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
    }

    @CEntryPoint(
            name = "dxfg_v4_DXFeed_attachSubscription",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_DXFeed_attachSubscription(
            final IsolateThread thread,
            final DxfgFeed feed,
            final DxfgSubscription<DXFeedSubscription<EventType<?>>> subscription
    ) {
        final Throwable stored = OlderEntryPoint.takeStoredException();
        final int result = FeedNative.dxfg_DXFeed_attachSubscription(thread, feed, subscription);

        return OlderEntryPoint.failed(stored, result == ExceptionHandlerReturnMinusOne.EXECUTE_FAIL)
                ? ExceptionHandlerReturnMinusOne.EXECUTE_FAIL
                : result;
    }

    @CEntryPoint(
            name = "dxfg_v4_DXFeed_detachSubscription",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_DXFeed_detachSubscription(
            final IsolateThread thread,
            final DxfgFeed feed,
            final DxfgSubscription<DXFeedSubscription<EventType<?>>> subscription
    ) {
        final Throwable stored = OlderEntryPoint.takeStoredException();
        final int result = FeedNative.dxfg_DXFeed_detachSubscription(thread, feed, subscription);

        return OlderEntryPoint.failed(stored, result == ExceptionHandlerReturnMinusOne.EXECUTE_FAIL)
                ? ExceptionHandlerReturnMinusOne.EXECUTE_FAIL
                : result;
    }

    @CEntryPoint(
            name = "dxfg_v4_DXFeed_detachSubscriptionAndClear",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_DXFeed_detachSubscriptionAndClear(
            final IsolateThread thread,
            final DxfgFeed feed,
            final DxfgSubscription<DXFeedSubscription<EventType<?>>> subscription
    ) {
        final Throwable stored = OlderEntryPoint.takeStoredException();
        final int result = FeedNative.dxfg_DXFeed_detachSubscriptionAndClear(thread, feed, subscription);

        return OlderEntryPoint.failed(stored, result == ExceptionHandlerReturnMinusOne.EXECUTE_FAIL)
                ? ExceptionHandlerReturnMinusOne.EXECUTE_FAIL
                : result;
    }

    @CEntryPoint(
            name = "dxfg_v4_DXFeed_getLastEventIfSubscribed",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_DXFeed_getLastEventIfSubscribed(
            final IsolateThread thread,
            final DxfgFeed feed,
            final DxfgEventClazz dxfgClazz,
            final DxfgSymbol dxfgSymbol,
            @DxfgOut final WordPointer event
    ) {
        if (event.isNull()) {
            throw new IllegalArgumentException("The `event` pointer is null");
        }

        final Throwable stored = OlderEntryPoint.takeStoredException();
        final DxfgEventType result = FeedNative.dxfg_DXFeed_getLastEventIfSubscribed(thread, feed, dxfgClazz, dxfgSymbol);

        if (OlderEntryPoint.failed(stored, result.isNull())) {
            return ExceptionHandlerReturnMinusOne.EXECUTE_FAIL;
        }

        event.write(result);

        return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
    }

    @CEntryPoint(
            name = "dxfg_v4_DXFeed_getIndexedEventsIfSubscribed",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_DXFeed_getIndexedEventsIfSubscribed(
            final IsolateThread thread,
            final DxfgFeed feed,
            final DxfgEventClazz dxfgClazz,
            final DxfgSymbol dxfgSymbol,
            final CCharPointer source,
            @DxfgOut final WordPointer events
    ) {
        if (events.isNull()) {
            throw new IllegalArgumentException("The `events` pointer is null");
        }

        final Throwable stored = OlderEntryPoint.takeStoredException();
        final DxfgEventTypeListPointer result = FeedNative.dxfg_DXFeed_getIndexedEventsIfSubscribed(
                        thread,
                        feed,
                        dxfgClazz,
                        dxfgSymbol,
                        source);

        if (OlderEntryPoint.failed(stored, result.isNull())) {
            return ExceptionHandlerReturnMinusOne.EXECUTE_FAIL;
        }

        events.write(result);

        return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
    }

    @CEntryPoint(
            name = "dxfg_v4_DXFeed_getTimeSeriesIfSubscribed",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_DXFeed_getTimeSeriesIfSubscribed(
            final IsolateThread thread,
            final DxfgFeed feed,
            final DxfgEventClazz dxfgClazz,
            final DxfgSymbol dxfgSymbol,
            final long fromTime,
            final long toTime,
            @DxfgOut final WordPointer events
    ) {
        if (events.isNull()) {
            throw new IllegalArgumentException("The `events` pointer is null");
        }

        final Throwable stored = OlderEntryPoint.takeStoredException();
        final DxfgEventTypeListPointer result = FeedNative.dxfg_DXFeed_getTimeSeriesIfSubscribed(
                        thread,
                        feed,
                        dxfgClazz,
                        dxfgSymbol,
                        fromTime,
                        toTime);

        if (OlderEntryPoint.failed(stored, result.isNull())) {
            return ExceptionHandlerReturnMinusOne.EXECUTE_FAIL;
        }

        events.write(result);

        return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
    }

    @CEntryPoint(
            name = "dxfg_v4_DXFeed_getLastEvent",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_DXFeed_getLastEvent(
            final IsolateThread thread,
            final DxfgFeed feed,
            @CConst final DxfgEventType nativeEvent,
            @DxfgOut final DxfgEventTypePointer lastEvent
    ) {
        final Throwable stored = OlderEntryPoint.takeStoredException();
        final int result = FeedNative.dxfg_DXFeed_getLastEvent2(thread, feed, nativeEvent, lastEvent);

        return OlderEntryPoint.failed(stored, result == ExceptionHandlerReturnMinusOne.EXECUTE_FAIL)
                ? ExceptionHandlerReturnMinusOne.EXECUTE_FAIL
                : result;
    }

    @CEntryPoint(
            name = "dxfg_v4_DXFeed_getLastEvents",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_DXFeed_getLastEvents(
            final IsolateThread thread,
            final DxfgFeed feed,
            @CConst final DxfgEventTypeListPointer nativeEvents,
            @DxfgOut final DxfgEventTypeListPointerPointer lastEvents
    ) {
        final Throwable stored = OlderEntryPoint.takeStoredException();
        final int result = FeedNative.dxfg_DXFeed_getLastEvents2(thread, feed, nativeEvents, lastEvents);

        return OlderEntryPoint.failed(stored, result == ExceptionHandlerReturnMinusOne.EXECUTE_FAIL)
                ? ExceptionHandlerReturnMinusOne.EXECUTE_FAIL
                : result;
    }

    @CEntryPoint(
            name = "dxfg_v4_DXFeedTimeSeriesSubscription_setFromTime",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_DXFeedTimeSeriesSubscription_setFromTime(
            final IsolateThread thread,
            final DxfgTimeSeriesSubscription dxfgTimeSeriesSubscription,
            final long fromTime
    ) {
        final Throwable stored = OlderEntryPoint.takeStoredException();
        final int result = FeedNative.dxfg_DXFeedTimeSeriesSubscription_setFromTime(
                        thread,
                        dxfgTimeSeriesSubscription,
                        fromTime);

        return OlderEntryPoint.failed(stored, result == ExceptionHandlerReturnMinusOne.EXECUTE_FAIL)
                ? ExceptionHandlerReturnMinusOne.EXECUTE_FAIL
                : result;
    }
}
