// Copyright (c) 2026 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

package com.dxfeed.sdk.feedfilter;

import com.dxfeed.api.DXEndpoint;
import com.dxfeed.api.DXFeedFilter;
import com.dxfeed.api.DXFeedFilterTracker;
import com.dxfeed.event.EventType;
import com.dxfeed.sdk.NativeUtils;
import com.dxfeed.sdk.common.DxfgOut;
import com.dxfeed.sdk.endpoint.DxfgEndpoint;
import com.dxfeed.sdk.events.DxfgEventClazz;
import com.dxfeed.sdk.events.DxfgEventType;
import com.dxfeed.sdk.exception.ExceptionHandlerReturnMinusOne;
import com.dxfeed.sdk.javac.DxfgCStringListPointer;
import com.dxfeed.sdk.symbol.DxfgSymbol;
import java.util.List;
import org.graalvm.nativeimage.CurrentIsolate;
import org.graalvm.nativeimage.IsolateThread;
import org.graalvm.nativeimage.c.CContext;
import org.graalvm.nativeimage.c.function.CEntryPoint;
import org.graalvm.nativeimage.c.type.CCharPointer;
import org.graalvm.nativeimage.c.type.CCharPointerPointer;
import org.graalvm.nativeimage.c.type.CConst;
import org.graalvm.nativeimage.c.type.CIntPointer;
import org.graalvm.nativeimage.c.type.VoidPointer;
import org.graalvm.nativeimage.c.type.WordPointer;
import org.graalvm.word.PointerBase;

/**
 * The functions of dxfg_v4_feed_filter.h: {@link DXFeedFilter}, its builder, tracker and listener (experimental).
 */
@CContext(FeedFilterDirectives.class)
public final class FeedFilterV4Native {

    private FeedFilterV4Native() {
    }

    @CEntryPoint(
            name = "dxfg_v4_DXFeedFilter_newBuilder",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_DXFeedFilter_newBuilder(
            final IsolateThread ignoredThread,
            final DxfgEndpoint endpoint,
            @DxfgOut final WordPointer builder
    ) {
        checkPointer(builder, "builder");
        checkHandle(endpoint, "endpoint");

        final DXEndpoint javaEndpoint = NativeUtils.MAPPER_ENDPOINT.toJava(endpoint);
        builder.write(NativeUtils.MAPPER_FEED_FILTER_BUILDER.toNative(DXFeedFilter.newBuilder(javaEndpoint)));

        return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
    }

    @CEntryPoint(
            name = "dxfg_v4_DXFeedFilter_Builder_withCategory",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_DXFeedFilter_Builder_withCategory(
            final IsolateThread ignoredThread,
            final DxfgFeedFilterBuilderHandle builder,
            @CConst final CCharPointer categoryName,
            @CConst final CCharPointer filterExpression
    ) {
        checkHandle(builder, "builder");

        FeedFilterCategories.add(NativeUtils.MAPPER_FEED_FILTER_BUILDER.toJava(builder),
                NativeUtils.MAPPER_STRING.toJava(categoryName), NativeUtils.MAPPER_STRING.toJava(filterExpression));

        return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
    }

    @CEntryPoint(
            name = "dxfg_v4_DXFeedFilter_Builder_withCategories",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_DXFeedFilter_Builder_withCategories(
            final IsolateThread ignoredThread,
            final DxfgFeedFilterBuilderHandle builder,
            @CConst final DxfgCStringListPointer categoryNames,
            @CConst final DxfgCStringListPointer filterExpressions
    ) {
        checkHandle(builder, "builder");

        FeedFilterCategories.addAll(NativeUtils.MAPPER_FEED_FILTER_BUILDER.toJava(builder),
                toJavaList(categoryNames), toJavaList(filterExpressions));

        return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
    }

    @CEntryPoint(
            name = "dxfg_v4_DXFeedFilter_Builder_build",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_DXFeedFilter_Builder_build(
            final IsolateThread ignoredThread,
            final DxfgFeedFilterBuilderHandle builder,
            @DxfgOut final WordPointer filter
    ) {
        checkPointer(filter, "filter");
        checkHandle(builder, "builder");

        filter.write(NativeUtils.MAPPER_FEED_FILTER.toNative(NativeUtils.MAPPER_FEED_FILTER_BUILDER.toJava(builder).build()));

        return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
    }

    @CEntryPoint(
            name = "dxfg_v4_DXFeedFilter_accept",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_DXFeedFilter_accept(
            final IsolateThread ignoredThread,
            final DxfgFeedFilterHandle filter,
            final DxfgEventClazz eventType,
            final DxfgSymbol symbol,
            @DxfgOut final CIntPointer accepted
    ) {
        checkPointer(accepted, "accepted");
        checkHandle(filter, "filter");
        checkPointer(symbol, "symbol");

        final Class<? extends EventType<?>> eventClass = eventType.eventClass();
        accepted.write(toInt(NativeUtils.MAPPER_FEED_FILTER.toJava(filter)
                .accept(eventClass, NativeUtils.MAPPER_SYMBOL.toJava(symbol))));

        return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
    }

    @CEntryPoint(
            name = "dxfg_v4_DXFeedFilter_acceptEvent",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_DXFeedFilter_acceptEvent(
            final IsolateThread ignoredThread,
            final DxfgFeedFilterHandle filter,
            final DxfgEventType event,
            @DxfgOut final CIntPointer accepted
    ) {
        checkPointer(accepted, "accepted");
        checkHandle(filter, "filter");
        checkPointer(event, "event");

        accepted.write(toInt(NativeUtils.MAPPER_FEED_FILTER.toJava(filter)
                .acceptEvent(NativeUtils.MAPPER_EVENT.toJava(event))));

        return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
    }

    @CEntryPoint(
            name = "dxfg_v4_DXFeedFilter_getCategory",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_DXFeedFilter_getCategory(
            final IsolateThread ignoredThread,
            final DxfgFeedFilterHandle filter,
            final DxfgEventClazz eventType,
            final DxfgSymbol symbol,
            @DxfgOut final CCharPointerPointer category
    ) {
        checkPointer(category, "category");
        checkHandle(filter, "filter");
        checkPointer(symbol, "symbol");

        final Class<? extends EventType<?>> eventClass = eventType.eventClass();
        // No match gives null, which the string mapper writes as NULL.
        category.write(NativeUtils.MAPPER_STRING.toNative(NativeUtils.MAPPER_FEED_FILTER.toJava(filter)
                .getCategory(eventClass, NativeUtils.MAPPER_SYMBOL.toJava(symbol))));

        return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
    }

    @CEntryPoint(
            name = "dxfg_v4_DXFeedFilter_getEventCategory",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_DXFeedFilter_getEventCategory(
            final IsolateThread ignoredThread,
            final DxfgFeedFilterHandle filter,
            final DxfgEventType event,
            @DxfgOut final CCharPointerPointer category
    ) {
        checkPointer(category, "category");
        checkHandle(filter, "filter");
        checkPointer(event, "event");

        category.write(NativeUtils.MAPPER_STRING.toNative(NativeUtils.MAPPER_FEED_FILTER.toJava(filter)
                .getEventCategory(NativeUtils.MAPPER_EVENT.toJava(event))));

        return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
    }

    @CEntryPoint(
            name = "dxfg_v4_DXFeedFilter_isDynamic",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_DXFeedFilter_isDynamic(
            final IsolateThread ignoredThread,
            final DxfgFeedFilterHandle filter,
            @DxfgOut final CIntPointer dynamic
    ) {
        checkPointer(dynamic, "dynamic");
        checkHandle(filter, "filter");

        dynamic.write(toInt(NativeUtils.MAPPER_FEED_FILTER.toJava(filter).isDynamic()));

        return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
    }

    @CEntryPoint(
            name = "dxfg_v4_DXFeedFilter_getTracker",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_DXFeedFilter_getTracker(
            final IsolateThread ignoredThread,
            final DxfgFeedFilterHandle filter,
            @DxfgOut final WordPointer tracker
    ) {
        checkPointer(tracker, "tracker");
        checkHandle(filter, "filter");

        tracker.write(NativeUtils.MAPPER_FEED_FILTER_TRACKER.toNative(
                NativeUtils.MAPPER_FEED_FILTER.toJava(filter).getTracker()));

        return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
    }

    @CEntryPoint(
            name = "dxfg_v4_DXFeedFilterTracker_getCurrentFilter",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_DXFeedFilterTracker_getCurrentFilter(
            final IsolateThread ignoredThread,
            final DxfgFeedFilterTrackerHandle tracker,
            @DxfgOut final WordPointer filter
    ) {
        checkPointer(filter, "filter");
        checkHandle(tracker, "tracker");

        filter.write(NativeUtils.MAPPER_FEED_FILTER.toNative(
                NativeUtils.MAPPER_FEED_FILTER_TRACKER.toJava(tracker).getCurrentFilter()));

        return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
    }

    @CEntryPoint(
            name = "dxfg_v4_DXFeedFilterTracker_addListener",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_DXFeedFilterTracker_addListener(
            final IsolateThread ignoredThread,
            final DxfgFeedFilterTrackerHandle tracker,
            final DxfgFeedFilterListenerHandle listener
    ) {
        checkHandle(tracker, "tracker");
        checkHandle(listener, "listener");

        final DXFeedFilterTracker javaTracker = NativeUtils.MAPPER_FEED_FILTER_TRACKER.toJava(tracker);
        javaTracker.addListener(NativeUtils.MAPPER_FEED_FILTER_LISTENER.toJava(listener));

        return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
    }

    @CEntryPoint(
            name = "dxfg_v4_DXFeedFilterTracker_removeListener",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_DXFeedFilterTracker_removeListener(
            final IsolateThread ignoredThread,
            final DxfgFeedFilterTrackerHandle tracker,
            final DxfgFeedFilterListenerHandle listener
    ) {
        checkHandle(tracker, "tracker");
        checkHandle(listener, "listener");

        final DXFeedFilterTracker javaTracker = NativeUtils.MAPPER_FEED_FILTER_TRACKER.toJava(tracker);
        javaTracker.removeListener(NativeUtils.MAPPER_FEED_FILTER_LISTENER.toJava(listener));

        return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
    }

    @CEntryPoint(
            name = "dxfg_v4_DXFeedFilterListener_new",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_DXFeedFilterListener_new(
            final IsolateThread ignoredThread,
            final DxfgFeedFilterListenerFunction userFunc,
            final VoidPointer userData,
            @DxfgOut final WordPointer listener
    ) {
        checkPointer(listener, "listener");

        if (userFunc.isNull()) {
            throw new IllegalArgumentException("The `user_func` function is null");
        }

        final class Listener extends FeedFilterListener {
            @Override
            protected void callNative() {
                // Only the user data: the function gets no handles that it would have to release.
                userFunc.invoke(CurrentIsolate.getCurrentThread(), userData);
            }
        }

        listener.write(NativeUtils.MAPPER_FEED_FILTER_LISTENER.toNative(new Listener()));

        return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
    }

    private static List<String> toJavaList(final DxfgCStringListPointer list) {
        if (list.isNull()) {
            return null; // FeedFilterCategories reports the null list by its name
        }
        if (list.getSize() > 0 && list.getElements().isNull()) {
            throw new IllegalArgumentException("The list of " + list.getSize() + " strings has no elements");
        }
        if (list.getSize() < 0) {
            throw new IllegalArgumentException("The list size is negative: " + list.getSize());
        }

        return NativeUtils.MAPPER_STRINGS.toJavaList(list);
    }

    private static void checkPointer(final PointerBase pointer, final String name) {
        if (pointer.isNull()) {
            throw new IllegalArgumentException("The `" + name + "` pointer is null");
        }
    }

    private static void checkHandle(final PointerBase handle, final String name) {
        if (handle.isNull()) {
            throw new IllegalArgumentException("The `" + name + "` handle is null");
        }
    }

    private static int toInt(final boolean value) {
        return value ? 1 : 0;
    }
}
