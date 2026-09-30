// Copyright (c) 2025 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

package com.dxfeed.sdk.feed;

import com.dxfeed.api.DXEndpoint;
import com.dxfeed.api.DXFeed;
import com.dxfeed.api.DXFeedSubscription;
import com.dxfeed.event.EventType;
import com.dxfeed.event.IndexedEvent;
import com.dxfeed.event.LastingEvent;
import com.dxfeed.event.TimeSeriesEvent;
import com.dxfeed.event.market.OrderSource;
import com.dxfeed.sdk.NativeUtils;
import com.dxfeed.sdk.common.DxfgOut;
import com.dxfeed.sdk.events.DxfgEventClazz;
import com.dxfeed.sdk.events.DxfgEventClazzList;
import com.dxfeed.sdk.events.DxfgEventType;
import com.dxfeed.sdk.events.DxfgEventTypeListPointer;
import com.dxfeed.sdk.events.DxfgEventTypeListPointerPointer;
import com.dxfeed.sdk.events.DxfgEventTypePointer;
import com.dxfeed.sdk.exception.ExceptionHandlerReturnMinusOne;
import com.dxfeed.sdk.exception.ExceptionHandlerReturnNullWord;
import com.dxfeed.sdk.subscription.DxfgSubscription;
import com.dxfeed.sdk.subscription.DxfgTimeSeriesSubscription;
import com.dxfeed.sdk.symbol.DxfgSymbol;
import com.dxfeed.sdk.system.IsolateResources;
import com.dxfeed.sdk.system.QdPropertyDefaults;
import java.util.ArrayList;
import java.util.List;
import org.graalvm.nativeimage.IsolateThread;
import org.graalvm.nativeimage.c.CContext;
import org.graalvm.nativeimage.c.function.CEntryPoint;
import org.graalvm.nativeimage.c.type.CCharPointer;
import org.graalvm.nativeimage.c.type.CConst;
import org.graalvm.word.WordFactory;

@CContext(FeedDirectives.class)
public class FeedNative {

    @CEntryPoint(
            name = "dxfg_DXFeed_getInstance",
            exceptionHandler = ExceptionHandlerReturnNullWord.class
    )
    public static DxfgFeed dxfg_DXFeed_getInstance(
            final IsolateThread ignoredThread
    ) {
        QdPropertyDefaults.apply();
        IsolateResources.register(DXEndpoint.getInstance()); // DXFeed.getInstance() is its feed
        return NativeUtils.MAPPER_FEED.toNative(DXFeed.getInstance());
    }

    @CEntryPoint(
            name = "dxfg_DXFeed_createSubscription",
            exceptionHandler = ExceptionHandlerReturnNullWord.class
    )
    public static DxfgSubscription<DXFeedSubscription<EventType<?>>> dxfg_DXFeed_createSubscription(
            final IsolateThread ignoredThread,
            final DxfgFeed dxfgFeed,
            final DxfgEventClazz dxfgClazz
    ) {
        return NativeUtils.MAPPER_SUBSCRIPTION.toNative(
                NativeUtils.MAPPER_FEED.toJava(dxfgFeed).createSubscription(dxfgClazz.clazz)
        );
    }

    @CEntryPoint(
            name = "dxfg_DXFeed_createSubscription2",
            exceptionHandler = ExceptionHandlerReturnNullWord.class
    )
    public static DxfgSubscription<DXFeedSubscription<EventType<?>>> dxfg_DXFeed_createSubscription2(
            final IsolateThread ignoredThread,
            final DxfgFeed dxfgFeed,
            final DxfgEventClazzList eventClazzList
    ) {
        return NativeUtils.MAPPER_SUBSCRIPTION.toNative(
                NativeUtils.MAPPER_FEED.toJava(dxfgFeed).createSubscription(
                        NativeUtils.MAPPER_EVENT_TYPES.toJavaList(eventClazzList).toArray(new Class[0])
                )
        );
    }

    @CEntryPoint(
            name = "dxfg_DXFeed_createTimeSeriesSubscription",
            exceptionHandler = ExceptionHandlerReturnNullWord.class
    )
    public static DxfgTimeSeriesSubscription dxfg_DXFeed_createTimeSeriesSubscription(
            final IsolateThread ignoredThread,
            final DxfgFeed feed,
            final DxfgEventClazz dxfgClazz
    ) {
        return NativeUtils.MAPPER_TIME_SERIES_SUBSCRIPTION.toNative(
                NativeUtils.MAPPER_FEED.toJava(feed)
                        .createTimeSeriesSubscription((Class<TimeSeriesEvent<?>>) dxfgClazz.clazz)
        );
    }

    @CEntryPoint(
            name = "dxfg_DXFeed_createTimeSeriesSubscription2",
            exceptionHandler = ExceptionHandlerReturnNullWord.class
    )
    public static DxfgTimeSeriesSubscription dxfg_DXFeed_createTimeSeriesSubscription2(
            final IsolateThread ignoredThread,
            final DxfgFeed feed,
            final DxfgEventClazzList eventClazzList
    ) {
        return NativeUtils.MAPPER_TIME_SERIES_SUBSCRIPTION.toNative(
                NativeUtils.MAPPER_FEED.toJava(feed).createTimeSeriesSubscription(
                        NativeUtils.MAPPER_EVENT_TYPES.toJavaList(eventClazzList).toArray(new Class[0])
                )
        );
    }

    @CEntryPoint(
            name = "dxfg_DXFeedTimeSeriesSubscription_setFromTime",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_DXFeedTimeSeriesSubscription_setFromTime(
            final IsolateThread ignoredThread,
            final DxfgTimeSeriesSubscription dxfgTimeSeriesSubscription,
            final long fromTime
    ) {
        NativeUtils.MAPPER_TIME_SERIES_SUBSCRIPTION.toJava(dxfgTimeSeriesSubscription).setFromTime(fromTime);
        return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
    }

    @CEntryPoint(
            name = "dxfg_DXFeed_attachSubscription",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_DXFeed_attachSubscription(
            final IsolateThread ignoredThread,
            final DxfgFeed feed,
            final DxfgSubscription<DXFeedSubscription<EventType<?>>> subscription
    ) {
        NativeUtils.MAPPER_FEED.toJava(feed).attachSubscription(NativeUtils.MAPPER_SUBSCRIPTION.toJava(subscription));
        return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
    }

    @CEntryPoint(
            name = "dxfg_DXFeed_detachSubscription",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_DXFeed_detachSubscription(
            final IsolateThread ignoredThread,
            final DxfgFeed feed,
            final DxfgSubscription<DXFeedSubscription<EventType<?>>> subscription
    ) {
        NativeUtils.MAPPER_FEED.toJava(feed).detachSubscription(NativeUtils.MAPPER_SUBSCRIPTION.toJava(subscription));
        return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
    }

    @CEntryPoint(
            name = "dxfg_DXFeed_detachSubscriptionAndClear",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_DXFeed_detachSubscriptionAndClear(
            final IsolateThread ignoredThread,
            final DxfgFeed feed,
            final DxfgSubscription<DXFeedSubscription<EventType<?>>> subscription
    ) {
        NativeUtils.MAPPER_FEED.toJava(feed)
                .detachSubscriptionAndClear(NativeUtils.MAPPER_SUBSCRIPTION.toJava(subscription));
        return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
    }

    @CEntryPoint(
            name = "dxfg_DXFeed_getLastEventIfSubscribed",
            exceptionHandler = ExceptionHandlerReturnNullWord.class
    )
    public static DxfgEventType dxfg_DXFeed_getLastEventIfSubscribed(
            final IsolateThread ignoredThread,
            final DxfgFeed feed,
            final DxfgEventClazz dxfgClazz,
            final DxfgSymbol dxfgSymbol
    ) {
        return NativeUtils.MAPPER_EVENT.toNative(
                NativeUtils.MAPPER_FEED.toJava(feed).getLastEventIfSubscribed(
                        (Class<LastingEvent<?>>) dxfgClazz.clazz,
                        NativeUtils.MAPPER_SYMBOL.toJava(dxfgSymbol)
                )
        );
    }

    @CEntryPoint(
            name = "dxfg_DXFeed_getIndexedEventsIfSubscribed",
            exceptionHandler = ExceptionHandlerReturnNullWord.class
    )
    public static DxfgEventTypeListPointer dxfg_DXFeed_getIndexedEventsIfSubscribed(
            final IsolateThread ignoredThread,
            final DxfgFeed feed,
            final DxfgEventClazz dxfgClazz,
            final DxfgSymbol dxfgSymbol,
            final CCharPointer source
    ) {
        return NativeUtils.MAPPER_EVENTS.toNativeList(
                NativeUtils.MAPPER_FEED.toJava(feed).getIndexedEventsIfSubscribed(
                        (Class<IndexedEvent<?>>) dxfgClazz.clazz,
                        NativeUtils.MAPPER_SYMBOL.toJava(dxfgSymbol),
                        OrderSource.valueOf(NativeUtils.MAPPER_STRING.toJava(source))
                )
        );
    }

    @CEntryPoint(
            name = "dxfg_DXFeed_getTimeSeriesIfSubscribed",
            exceptionHandler = ExceptionHandlerReturnNullWord.class
    )
    public static DxfgEventTypeListPointer dxfg_DXFeed_getTimeSeriesIfSubscribed(
            final IsolateThread ignoredThread,
            final DxfgFeed feed,
            final DxfgEventClazz dxfgClazz,
            final DxfgSymbol dxfgSymbol,
            final long fromTime,
            final long toTime
    ) {
        return NativeUtils.MAPPER_EVENTS.toNativeList(
                NativeUtils.MAPPER_FEED.toJava(feed).getTimeSeriesIfSubscribed(
                        (Class<TimeSeriesEvent<?>>) dxfgClazz.clazz,
                        NativeUtils.MAPPER_SYMBOL.toJava(dxfgSymbol),
                        fromTime,
                        toTime
                )
        );
    }

    @CEntryPoint(
            name = "dxfg_DXFeed_getLastEvent",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_DXFeed_getLastEvent(
            final IsolateThread ignoredThread,
            final DxfgFeed feed,
            final DxfgEventType nativeEvent
    ) {
        final LastingEvent<?> javaEvent = (LastingEvent<?>) NativeUtils.MAPPER_EVENT.toJava(nativeEvent);
        NativeUtils.MAPPER_FEED.toJava(feed).getLastEvent(javaEvent);
        NativeUtils.MAPPER_EVENT.fillNative(javaEvent, nativeEvent, true);
        return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
    }

    @CEntryPoint(
            name = "dxfg_DXFeed_getLastEvents",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_DXFeed_getLastEvents(
            final IsolateThread ignoredThread,
            final DxfgFeed feed,
            final DxfgEventTypeListPointer events
    ) {
        for (int i = 0; i < events.getSize(); i++) {
            final DxfgEventType nativeEvent = events.getElements().addressOf(i).read();
            final LastingEvent<?> javaEvent = (LastingEvent<?>) NativeUtils.MAPPER_EVENT.toJava(nativeEvent);
            NativeUtils.MAPPER_FEED.toJava(feed).getLastEvent(javaEvent);
            NativeUtils.MAPPER_EVENT.fillNative(javaEvent, nativeEvent, true);
        }
        return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
    }

    // The given event is only read (the caller owns it), the result is a new event: the last event, or the copy of the
    // given event when the last event is not available (as DXFeed.getLastEvent leaves the event unchanged then).
    @CEntryPoint(
            name = "dxfg_DXFeed_getLastEvent2",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_DXFeed_getLastEvent2(
            final IsolateThread ignoredThread,
            final DxfgFeed feed,
            @CConst final DxfgEventType nativeEvent,
            @DxfgOut final DxfgEventTypePointer lastEvent
    ) {
        if (lastEvent.isNull()) {
            throw new IllegalArgumentException("The `lastEvent` pointer is null");
        }

        // NULL on error
        lastEvent.write(WordFactory.nullPointer());

        final LastingEvent<?> javaEvent = (LastingEvent<?>) NativeUtils.MAPPER_EVENT.toJava(nativeEvent);

        if (javaEvent == null) {
            throw new IllegalArgumentException("The `event` pointer is null");
        }

        lastEvent.write(
                NativeUtils.MAPPER_EVENT.toNative(NativeUtils.MAPPER_FEED.toJava(feed).getLastEvent(javaEvent)));

        return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
    }

    // The given list and its events are only read, the result is a new list of new events
    // (see dxfg_DXFeed_getLastEvent2).
    @CEntryPoint(
            name = "dxfg_DXFeed_getLastEvents2",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_DXFeed_getLastEvents2(
            final IsolateThread ignoredThread,
            final DxfgFeed feed,
            @CConst final DxfgEventTypeListPointer nativeEvents,
            @DxfgOut final DxfgEventTypeListPointerPointer lastEvents
    ) {
        if (lastEvents.isNull()) {
            throw new IllegalArgumentException("The `lastEvents` pointer is null");
        }

        // NULL on error
        lastEvents.write(WordFactory.nullPointer());

        if (nativeEvents.isNull()) {
            throw new IllegalArgumentException("The `events` pointer is null");
        }

        final List<LastingEvent<?>> javaEvents = new ArrayList<>(nativeEvents.getSize());

        for (final EventType<?> javaEvent : NativeUtils.MAPPER_EVENTS.toJavaList(nativeEvents)) {
            if (javaEvent == null) {
                throw new IllegalArgumentException("The `events` list contains a null event");
            }

            javaEvents.add((LastingEvent<?>) javaEvent);
        }

        lastEvents.write(
                NativeUtils.MAPPER_EVENTS.toNativeList(NativeUtils.MAPPER_FEED.toJava(feed).getLastEvents(javaEvents)));

        return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
    }
}
