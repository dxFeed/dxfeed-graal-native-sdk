// Copyright (c) 2025 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

package com.dxfeed.sdk.feed;

import com.dxfeed.api.DXFeed;
import com.dxfeed.api.DXFeedSubscription;
import com.dxfeed.event.EventType;
import com.dxfeed.event.IndexedEvent;
import com.dxfeed.event.LastingEvent;
import com.dxfeed.event.TimeSeriesEvent;
import com.dxfeed.event.market.OrderSource;
import com.dxfeed.sdk.NativeUtils;
import com.dxfeed.sdk.events.DxfgEventClazz;
import com.dxfeed.sdk.events.DxfgEventClazzList;
import com.dxfeed.sdk.events.DxfgEventType;
import com.dxfeed.sdk.events.DxfgEventTypeListPointer;
import com.dxfeed.sdk.exception.ExceptionHandlerReturnMinusOne;
import com.dxfeed.sdk.exception.ExceptionHandlerReturnNullWord;
import com.dxfeed.sdk.subscription.DxfgSubscription;
import com.dxfeed.sdk.subscription.DxfgTimeSeriesSubscription;
import com.dxfeed.sdk.symbol.DxfgSymbol;
import org.graalvm.nativeimage.IsolateThread;
import org.graalvm.nativeimage.c.CContext;
import org.graalvm.nativeimage.c.function.CEntryPoint;
import org.graalvm.nativeimage.c.type.CCharPointer;

@CContext(Directives.class)
public class FeedNative {

    @CEntryPoint(
            name = "dxfg_DXFeed_getInstance",
            exceptionHandler = ExceptionHandlerReturnNullWord.class
    )
    public static DxfgFeed dxfg_DXFeed_getInstance(
            final IsolateThread ignoredThread
    ) {
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
            final DxfgEventType nEvent
    ) {
        final LastingEvent<?> jEvent = (LastingEvent<?>) NativeUtils.MAPPER_EVENT.toJava(nEvent);
        NativeUtils.MAPPER_FEED.toJava(feed).getLastEvent(jEvent);
        NativeUtils.MAPPER_EVENT.fillNative(jEvent, nEvent, true);
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
            final DxfgEventType nEvent = events.getElements().addressOf(i).read();
            final LastingEvent<?> jEvent = (LastingEvent<?>) NativeUtils.MAPPER_EVENT.toJava(nEvent);
            NativeUtils.MAPPER_FEED.toJava(feed).getLastEvent(jEvent);
            NativeUtils.MAPPER_EVENT.fillNative(jEvent, nEvent, true);
        }
        return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
    }
}
