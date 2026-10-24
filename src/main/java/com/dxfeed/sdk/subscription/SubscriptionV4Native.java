// Copyright (c) 2026 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

package com.dxfeed.sdk.subscription;

import com.dxfeed.api.DXFeedSubscription;
import com.dxfeed.event.EventType;
import com.dxfeed.sdk.common.CInt32Pointer;
import com.dxfeed.sdk.common.DxfgOut;
import com.dxfeed.sdk.events.DxfgEventClazz;
import com.dxfeed.sdk.events.DxfgEventClazzList;
import com.dxfeed.sdk.events.DxfgObservableSubscriptionChangeListener;
import com.dxfeed.sdk.events.DxfgObservableSubscriptionChangeListenerFunctionSubscriptionClosed;
import com.dxfeed.sdk.events.DxfgObservableSubscriptionChangeListenerFunctionSymbolsAdded;
import com.dxfeed.sdk.events.DxfgObservableSubscriptionChangeListenerFunctionSymbolsRemoved;
import com.dxfeed.sdk.exception.ExceptionHandlerReturnMinusOne;
import com.dxfeed.sdk.exception.OlderEntryPoint;
import com.dxfeed.sdk.feed.DxfgFeed;
import com.dxfeed.sdk.javac.DxfgExecutorHandle;
import com.dxfeed.sdk.javac.DxfgTimePeriodHandle;
import com.dxfeed.sdk.subscriptioncontroller.DxfgSubscriptionControllerHandlePointer;
import com.dxfeed.sdk.symbol.DxfgSymbol;
import com.dxfeed.sdk.symbol.DxfgSymbolList;
import org.graalvm.nativeimage.IsolateThread;
import org.graalvm.nativeimage.c.CContext;
import org.graalvm.nativeimage.c.function.CEntryPoint;
import org.graalvm.nativeimage.c.type.CLongPointer;
import org.graalvm.nativeimage.c.type.VoidPointer;
import org.graalvm.nativeimage.c.type.WordPointer;

/**
 * The v4 twins of the functions of {@link SubscriptionNative}: each one calls the old function as a Java method
 * ({@link OlderEntryPoint} tells whether it failed) and returns its result through an out parameter (see
 * dxfg_v4_api.h).
 */
@CContext(SubscriptionDirectives.class)
public final class SubscriptionV4Native {

    private SubscriptionV4Native() {
    }

    @CEntryPoint(
            name = "dxfg_v4_DXFeedSubscription_new",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_DXFeedSubscription_new(
            final IsolateThread thread,
            final DxfgEventClazz dxfgClazz,
            @DxfgOut final WordPointer subscription
    ) {
        if (subscription.isNull()) {
            throw new IllegalArgumentException("The `subscription` pointer is null");
        }

        final Throwable stored = OlderEntryPoint.takeStoredException();
        final DxfgSubscription<DXFeedSubscription<EventType<?>>> result = SubscriptionNative.dxfg_DXFeedSubscription_new(thread, dxfgClazz);

        if (OlderEntryPoint.failed(stored, result.isNull())) {
            return ExceptionHandlerReturnMinusOne.EXECUTE_FAIL;
        }

        subscription.write(result);

        return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
    }

    @CEntryPoint(
            name = "dxfg_v4_DXFeedSubscription_newWithEventTypes",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_DXFeedSubscription_newWithEventTypes(
            final IsolateThread thread,
            final DxfgEventClazzList eventClazzList,
            @DxfgOut final WordPointer subscription
    ) {
        if (subscription.isNull()) {
            throw new IllegalArgumentException("The `subscription` pointer is null");
        }

        final Throwable stored = OlderEntryPoint.takeStoredException();
        final DxfgSubscription<DXFeedSubscription<EventType<?>>> result = SubscriptionNative.dxfg_DXFeedSubscription_new(thread, eventClazzList);

        if (OlderEntryPoint.failed(stored, result.isNull())) {
            return ExceptionHandlerReturnMinusOne.EXECUTE_FAIL;
        }

        subscription.write(result);

        return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
    }

    @CEntryPoint(
            name = "dxfg_v4_DXFeedSubscription_getSubscriptionController",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_DXFeedSubscription_getSubscriptionController(
            final IsolateThread thread,
            final DxfgSubscription<DXFeedSubscription<EventType<?>>> dxfgSubscription,
            @DxfgOut final DxfgSubscriptionControllerHandlePointer subscriptionController
    ) {
        final Throwable stored = OlderEntryPoint.takeStoredException();
        final int result = SubscriptionNative.dxfg_DXFeedSubscription_getSubscriptionController(
                        thread,
                        dxfgSubscription,
                        subscriptionController);

        return OlderEntryPoint.failed(stored, result == ExceptionHandlerReturnMinusOne.EXECUTE_FAIL)
                ? ExceptionHandlerReturnMinusOne.EXECUTE_FAIL
                : result;
    }

    @CEntryPoint(
            name = "dxfg_v4_DXFeedSubscription_attach",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_DXFeedSubscription_attach(
            final IsolateThread thread,
            final DxfgSubscription<DXFeedSubscription<EventType<?>>> dxfgSubscription,
            final DxfgFeed feed
    ) {
        final Throwable stored = OlderEntryPoint.takeStoredException();
        final int result = SubscriptionNative.dxfg_DXFeedSubscription_attach(thread, dxfgSubscription, feed);

        return OlderEntryPoint.failed(stored, result == ExceptionHandlerReturnMinusOne.EXECUTE_FAIL)
                ? ExceptionHandlerReturnMinusOne.EXECUTE_FAIL
                : result;
    }

    @CEntryPoint(
            name = "dxfg_v4_DXFeedSubscription_detach",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_DXFeedSubscription_detach(
            final IsolateThread thread,
            final DxfgSubscription<DXFeedSubscription<EventType<?>>> dxfgSubscription,
            final DxfgFeed feed
    ) {
        final Throwable stored = OlderEntryPoint.takeStoredException();
        final int result = SubscriptionNative.dxfg_DXFeedSubscription_detach(thread, dxfgSubscription, feed);

        return OlderEntryPoint.failed(stored, result == ExceptionHandlerReturnMinusOne.EXECUTE_FAIL)
                ? ExceptionHandlerReturnMinusOne.EXECUTE_FAIL
                : result;
    }

    @CEntryPoint(
            name = "dxfg_v4_DXFeedSubscription_isClosed",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_DXFeedSubscription_isClosed(
            final IsolateThread thread,
            final DxfgSubscription<DXFeedSubscription<EventType<?>>> dxfgSubscription,
            @DxfgOut final CInt32Pointer closed
    ) {
        if (closed.isNull()) {
            throw new IllegalArgumentException("The `closed` pointer is null");
        }

        final Throwable stored = OlderEntryPoint.takeStoredException();
        final int result = SubscriptionNative.dxfg_DXFeedSubscription_isClosed(thread, dxfgSubscription);

        if (OlderEntryPoint.failed(stored, result == ExceptionHandlerReturnMinusOne.EXECUTE_FAIL)) {
            return ExceptionHandlerReturnMinusOne.EXECUTE_FAIL;
        }

        closed.write(result);

        return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
    }

    @CEntryPoint(
            name = "dxfg_v4_DXFeedSubscription_close",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_DXFeedSubscription_close(
            final IsolateThread thread,
            final DxfgSubscription<DXFeedSubscription<EventType<?>>> dxfgSubscription
    ) {
        final Throwable stored = OlderEntryPoint.takeStoredException();
        final int result = SubscriptionNative.dxfg_DXFeedSubscription_close(thread, dxfgSubscription);

        return OlderEntryPoint.failed(stored, result == ExceptionHandlerReturnMinusOne.EXECUTE_FAIL)
                ? ExceptionHandlerReturnMinusOne.EXECUTE_FAIL
                : result;
    }

    @CEntryPoint(
            name = "dxfg_v4_DXFeedSubscription_getEventTypes",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_DXFeedSubscription_getEventTypes(
            final IsolateThread thread,
            final DxfgSubscription<DXFeedSubscription<EventType<?>>> dxfgSubscription,
            @DxfgOut final WordPointer eventTypes
    ) {
        if (eventTypes.isNull()) {
            throw new IllegalArgumentException("The `eventTypes` pointer is null");
        }

        final Throwable stored = OlderEntryPoint.takeStoredException();
        final DxfgEventClazzList result = SubscriptionNative.dxfg_DXFeedSubscription_getEventTypes(thread, dxfgSubscription);

        if (OlderEntryPoint.failed(stored, result.isNull())) {
            return ExceptionHandlerReturnMinusOne.EXECUTE_FAIL;
        }

        eventTypes.write(result);

        return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
    }

    @CEntryPoint(
            name = "dxfg_v4_DXFeedSubscription_containsEventType",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_DXFeedSubscription_containsEventType(
            final IsolateThread thread,
            final DxfgSubscription<DXFeedSubscription<EventType<?>>> dxfgSubscription,
            final DxfgEventClazz dxfgClazz,
            @DxfgOut final CInt32Pointer contains
    ) {
        if (contains.isNull()) {
            throw new IllegalArgumentException("The `contains` pointer is null");
        }

        final Throwable stored = OlderEntryPoint.takeStoredException();
        final int result = SubscriptionNative.dxfg_DXFeedSubscription_containsEventType(
                        thread,
                        dxfgSubscription,
                        dxfgClazz);

        if (OlderEntryPoint.failed(stored, result == ExceptionHandlerReturnMinusOne.EXECUTE_FAIL)) {
            return ExceptionHandlerReturnMinusOne.EXECUTE_FAIL;
        }

        contains.write(result);

        return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
    }

    @CEntryPoint(
            name = "dxfg_v4_DXFeedSubscription_clear",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_DXFeedSubscription_clear(
            final IsolateThread thread,
            final DxfgSubscription<DXFeedSubscription<EventType<?>>> dxfgSubscription
    ) {
        final Throwable stored = OlderEntryPoint.takeStoredException();
        final int result = SubscriptionNative.dxfg_DXFeedSubscription_clear(thread, dxfgSubscription);

        return OlderEntryPoint.failed(stored, result == ExceptionHandlerReturnMinusOne.EXECUTE_FAIL)
                ? ExceptionHandlerReturnMinusOne.EXECUTE_FAIL
                : result;
    }

    @CEntryPoint(
            name = "dxfg_v4_DXFeedSubscription_getSymbols",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_DXFeedSubscription_getSymbols(
            final IsolateThread thread,
            final DxfgSubscription<DXFeedSubscription<EventType<?>>> dxfgSubscription,
            @DxfgOut final WordPointer symbols
    ) {
        if (symbols.isNull()) {
            throw new IllegalArgumentException("The `symbols` pointer is null");
        }

        final Throwable stored = OlderEntryPoint.takeStoredException();
        final DxfgSymbolList result = SubscriptionNative.dxfg_DXFeedSubscription_getSymbols(thread, dxfgSubscription);

        if (OlderEntryPoint.failed(stored, result.isNull())) {
            return ExceptionHandlerReturnMinusOne.EXECUTE_FAIL;
        }

        symbols.write(result);

        return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
    }

    @CEntryPoint(
            name = "dxfg_v4_DXFeedSubscription_getDecoratedSymbols",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_DXFeedSubscription_getDecoratedSymbols(
            final IsolateThread thread,
            final DxfgSubscription<DXFeedSubscription<EventType<?>>> dxfgSubscription,
            @DxfgOut final WordPointer symbols
    ) {
        if (symbols.isNull()) {
            throw new IllegalArgumentException("The `symbols` pointer is null");
        }

        final Throwable stored = OlderEntryPoint.takeStoredException();
        final DxfgSymbolList result = SubscriptionNative.dxfg_DXFeedSubscription_getDecoratedSymbols(thread, dxfgSubscription);

        if (OlderEntryPoint.failed(stored, result.isNull())) {
            return ExceptionHandlerReturnMinusOne.EXECUTE_FAIL;
        }

        symbols.write(result);

        return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
    }

    @CEntryPoint(
            name = "dxfg_v4_DXFeedSubscription_setSymbol",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_DXFeedSubscription_setSymbol(
            final IsolateThread thread,
            final DxfgSubscription<DXFeedSubscription<EventType<?>>> dxfgSubscription,
            final DxfgSymbol dxfgSymbol
    ) {
        final Throwable stored = OlderEntryPoint.takeStoredException();
        final int result = SubscriptionNative.dxfg_DXFeedSubscription_setSymbol(thread, dxfgSubscription, dxfgSymbol);

        return OlderEntryPoint.failed(stored, result == ExceptionHandlerReturnMinusOne.EXECUTE_FAIL)
                ? ExceptionHandlerReturnMinusOne.EXECUTE_FAIL
                : result;
    }

    @CEntryPoint(
            name = "dxfg_v4_DXFeedSubscription_setSymbols",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_DXFeedSubscription_setSymbols(
            final IsolateThread thread,
            final DxfgSubscription<DXFeedSubscription<EventType<?>>> dxfgSubscription,
            final DxfgSymbolList dxfgSymbolList
    ) {
        final Throwable stored = OlderEntryPoint.takeStoredException();
        final int result = SubscriptionNative.dxfg_DXFeedSubscription_setSymbols(
                        thread,
                        dxfgSubscription,
                        dxfgSymbolList);

        return OlderEntryPoint.failed(stored, result == ExceptionHandlerReturnMinusOne.EXECUTE_FAIL)
                ? ExceptionHandlerReturnMinusOne.EXECUTE_FAIL
                : result;
    }

    @CEntryPoint(
            name = "dxfg_v4_DXFeedSubscription_addSymbol",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_DXFeedSubscription_addSymbol(
            final IsolateThread thread,
            final DxfgSubscription<DXFeedSubscription<EventType<?>>> dxfgSubscription,
            final DxfgSymbol dxfgSymbol
    ) {
        final Throwable stored = OlderEntryPoint.takeStoredException();
        final int result = SubscriptionNative.dxfg_DXFeedSubscription_addSymbol(thread, dxfgSubscription, dxfgSymbol);

        return OlderEntryPoint.failed(stored, result == ExceptionHandlerReturnMinusOne.EXECUTE_FAIL)
                ? ExceptionHandlerReturnMinusOne.EXECUTE_FAIL
                : result;
    }

    @CEntryPoint(
            name = "dxfg_v4_DXFeedSubscription_addSymbols",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_DXFeedSubscription_addSymbols(
            final IsolateThread thread,
            final DxfgSubscription<DXFeedSubscription<EventType<?>>> dxfgSubscription,
            final DxfgSymbolList symbols
    ) {
        final Throwable stored = OlderEntryPoint.takeStoredException();
        final int result = SubscriptionNative.dxfg_DXFeedSubscription_addSymbols(thread, dxfgSubscription, symbols);

        return OlderEntryPoint.failed(stored, result == ExceptionHandlerReturnMinusOne.EXECUTE_FAIL)
                ? ExceptionHandlerReturnMinusOne.EXECUTE_FAIL
                : result;
    }

    @CEntryPoint(
            name = "dxfg_v4_DXFeedSubscription_removeSymbol",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_DXFeedSubscription_removeSymbol(
            final IsolateThread thread,
            final DxfgSubscription<DXFeedSubscription<EventType<?>>> dxfgSubscription,
            final DxfgSymbol dxfgSymbol
    ) {
        final Throwable stored = OlderEntryPoint.takeStoredException();
        final int result = SubscriptionNative.dxfg_DXFeedSubscription_removeSymbol(
                        thread,
                        dxfgSubscription,
                        dxfgSymbol);

        return OlderEntryPoint.failed(stored, result == ExceptionHandlerReturnMinusOne.EXECUTE_FAIL)
                ? ExceptionHandlerReturnMinusOne.EXECUTE_FAIL
                : result;
    }

    @CEntryPoint(
            name = "dxfg_v4_DXFeedSubscription_removeSymbols",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_DXFeedSubscription_removeSymbols(
            final IsolateThread thread,
            final DxfgSubscription<DXFeedSubscription<EventType<?>>> dxfgSubscription,
            final DxfgSymbolList symbols
    ) {
        final Throwable stored = OlderEntryPoint.takeStoredException();
        final int result = SubscriptionNative.dxfg_DXFeedSubscription_removeSymbols(
                        thread,
                        dxfgSubscription,
                        symbols);

        return OlderEntryPoint.failed(stored, result == ExceptionHandlerReturnMinusOne.EXECUTE_FAIL)
                ? ExceptionHandlerReturnMinusOne.EXECUTE_FAIL
                : result;
    }

    @CEntryPoint(
            name = "dxfg_v4_DXFeedSubscription_getExecutor",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_DXFeedSubscription_getExecutor(
            final IsolateThread thread,
            final DxfgSubscription<DXFeedSubscription<EventType<?>>> dxfgSubscription,
            @DxfgOut final WordPointer executor
    ) {
        if (executor.isNull()) {
            throw new IllegalArgumentException("The `executor` pointer is null");
        }

        final Throwable stored = OlderEntryPoint.takeStoredException();
        final DxfgExecutorHandle result = SubscriptionNative.dxfg_DXFeedSubscription_getExecutor(thread, dxfgSubscription);

        if (OlderEntryPoint.failed(stored, result.isNull())) {
            return ExceptionHandlerReturnMinusOne.EXECUTE_FAIL;
        }

        executor.write(result);

        return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
    }

    @CEntryPoint(
            name = "dxfg_v4_DXFeedSubscription_setExecutor",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_DXFeedSubscription_setExecutor(
            final IsolateThread thread,
            final DxfgSubscription<DXFeedSubscription<EventType<?>>> dxfgSubscription,
            final DxfgExecutorHandle executor
    ) {
        final Throwable stored = OlderEntryPoint.takeStoredException();
        final int result = SubscriptionNative.dxfg_DXFeedSubscription_setExecutor(thread, dxfgSubscription, executor);

        return OlderEntryPoint.failed(stored, result == ExceptionHandlerReturnMinusOne.EXECUTE_FAIL)
                ? ExceptionHandlerReturnMinusOne.EXECUTE_FAIL
                : result;
    }

    @CEntryPoint(
            name = "dxfg_v4_DXFeedSubscription_getAggregationPeriod",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_DXFeedSubscription_getAggregationPeriod(
            final IsolateThread thread,
            final DxfgSubscription<DXFeedSubscription<EventType<?>>> dxfgSubscription,
            @DxfgOut final WordPointer period
    ) {
        if (period.isNull()) {
            throw new IllegalArgumentException("The `period` pointer is null");
        }

        final Throwable stored = OlderEntryPoint.takeStoredException();
        final DxfgTimePeriodHandle result = SubscriptionNative.dxfg_DXFeedSubscription_getAggregationPeriod(thread, dxfgSubscription);

        if (OlderEntryPoint.failed(stored, result.isNull())) {
            return ExceptionHandlerReturnMinusOne.EXECUTE_FAIL;
        }

        period.write(result);

        return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
    }

    @CEntryPoint(
            name = "dxfg_v4_DXFeedSubscription_getAggregationPeriodMillis",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_DXFeedSubscription_getAggregationPeriodMillis(
            final IsolateThread thread,
            final DxfgSubscription<DXFeedSubscription<EventType<?>>> dxfgSubscription,
            @DxfgOut final CLongPointer aggregationPeriod
    ) {
        final Throwable stored = OlderEntryPoint.takeStoredException();
        final int result = SubscriptionNative.dxfg_DXFeedSubscription_getAggregationPeriodMillis(
                        thread,
                        dxfgSubscription,
                        aggregationPeriod);

        return OlderEntryPoint.failed(stored, result == ExceptionHandlerReturnMinusOne.EXECUTE_FAIL)
                ? ExceptionHandlerReturnMinusOne.EXECUTE_FAIL
                : result;
    }

    @CEntryPoint(
            name = "dxfg_v4_DXFeedSubscription_setAggregationPeriod",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_DXFeedSubscription_setAggregationPeriod(
            final IsolateThread thread,
            final DxfgSubscription<DXFeedSubscription<EventType<?>>> dxfgSubscription,
            final DxfgTimePeriodHandle timePeriod
    ) {
        final Throwable stored = OlderEntryPoint.takeStoredException();
        final int result = SubscriptionNative.dxfg_DXFeedSubscription_setAggregationPeriod(
                        thread,
                        dxfgSubscription,
                        timePeriod);

        return OlderEntryPoint.failed(stored, result == ExceptionHandlerReturnMinusOne.EXECUTE_FAIL)
                ? ExceptionHandlerReturnMinusOne.EXECUTE_FAIL
                : result;
    }

    @CEntryPoint(
            name = "dxfg_v4_DXFeedSubscription_setAggregationPeriodMillis",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_DXFeedSubscription_setAggregationPeriodMillis(
            final IsolateThread thread,
            final DxfgSubscription<DXFeedSubscription<EventType<?>>> dxfgSubscription,
            final long timePeriod
    ) {
        final Throwable stored = OlderEntryPoint.takeStoredException();
        final int result = SubscriptionNative.dxfg_DXFeedSubscription_setAggregationPeriodMillis(
                        thread,
                        dxfgSubscription,
                        timePeriod);

        return OlderEntryPoint.failed(stored, result == ExceptionHandlerReturnMinusOne.EXECUTE_FAIL)
                ? ExceptionHandlerReturnMinusOne.EXECUTE_FAIL
                : result;
    }

    @CEntryPoint(
            name = "dxfg_v4_DXFeedSubscription_getAggregationPeriodInfo",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_DXFeedSubscription_getAggregationPeriodInfo(
            final IsolateThread thread,
            final DxfgSubscription<DXFeedSubscription<EventType<?>>> dxfgSubscription,
            @DxfgOut final CLongPointer min,
            @DxfgOut final CLongPointer max
    ) {
        final Throwable stored = OlderEntryPoint.takeStoredException();
        final int result = SubscriptionNative.dxfg_DXFeedSubscription_getAggregationPeriodInfo(
                        thread,
                        dxfgSubscription,
                        min,
                        max);

        return OlderEntryPoint.failed(stored, result == ExceptionHandlerReturnMinusOne.EXECUTE_FAIL)
                ? ExceptionHandlerReturnMinusOne.EXECUTE_FAIL
                : result;
    }

    @CEntryPoint(
            name = "dxfg_v4_DXFeedSubscription_getTotalDroppedEvents",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_DXFeedSubscription_getTotalDroppedEvents(
            final IsolateThread thread,
            final DxfgSubscription<DXFeedSubscription<EventType<?>>> dxfgSubscription,
            @DxfgOut final CLongPointer droppedEvents
    ) {
        final Throwable stored = OlderEntryPoint.takeStoredException();
        final int result = SubscriptionNative.dxfg_DXFeedSubscription_getTotalDroppedEvents(
                        thread,
                        dxfgSubscription,
                        droppedEvents);

        return OlderEntryPoint.failed(stored, result == ExceptionHandlerReturnMinusOne.EXECUTE_FAIL)
                ? ExceptionHandlerReturnMinusOne.EXECUTE_FAIL
                : result;
    }

    @CEntryPoint(
            name = "dxfg_v4_DXFeedEventListener_new",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_DXFeedEventListener_new(
            final IsolateThread thread,
            final DxfgFeedEventListenerFunction dxfgFeedEventListenerFunction,
            final VoidPointer userData,
            @DxfgOut final WordPointer listener
    ) {
        if (listener.isNull()) {
            throw new IllegalArgumentException("The `listener` pointer is null");
        }

        final Throwable stored = OlderEntryPoint.takeStoredException();
        final DxfgFeedEventListener result = SubscriptionNative.dxfg_DXFeedEventListener_new(
                        thread,
                        dxfgFeedEventListenerFunction,
                        userData);

        if (OlderEntryPoint.failed(stored, result.isNull())) {
            return ExceptionHandlerReturnMinusOne.EXECUTE_FAIL;
        }

        listener.write(result);

        return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
    }

    @CEntryPoint(
            name = "dxfg_v4_DXFeedSubscription_addEventListener",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_DXFeedSubscription_addEventListener(
            final IsolateThread thread,
            final DxfgSubscription<DXFeedSubscription<EventType<?>>> dxfgSubscription,
            final DxfgFeedEventListener dxfgFeedEventListener
    ) {
        final Throwable stored = OlderEntryPoint.takeStoredException();
        final int result = SubscriptionNative.dxfg_DXFeedSubscription_addEventListener(
                        thread,
                        dxfgSubscription,
                        dxfgFeedEventListener);

        return OlderEntryPoint.failed(stored, result == ExceptionHandlerReturnMinusOne.EXECUTE_FAIL)
                ? ExceptionHandlerReturnMinusOne.EXECUTE_FAIL
                : result;
    }

    @CEntryPoint(
            name = "dxfg_v4_DXFeedSubscription_removeEventListener",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_DXFeedSubscription_removeEventListener(
            final IsolateThread thread,
            final DxfgSubscription<DXFeedSubscription<EventType<?>>> dxfgSubscription,
            final DxfgFeedEventListener dxfgFeedEventListener
    ) {
        final Throwable stored = OlderEntryPoint.takeStoredException();
        final int result = SubscriptionNative.dxfg_DXFeedSubscription_removeEventListener(
                        thread,
                        dxfgSubscription,
                        dxfgFeedEventListener);

        return OlderEntryPoint.failed(stored, result == ExceptionHandlerReturnMinusOne.EXECUTE_FAIL)
                ? ExceptionHandlerReturnMinusOne.EXECUTE_FAIL
                : result;
    }

    @CEntryPoint(
            name = "dxfg_v4_DXFeedSubscription_addChangeListener",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_DXFeedSubscription_addChangeListener(
            final IsolateThread thread,
            final DxfgSubscription<DXFeedSubscription<EventType<?>>> dxfgSubscription,
            final DxfgObservableSubscriptionChangeListener dxfgFeedEventListener
    ) {
        final Throwable stored = OlderEntryPoint.takeStoredException();
        final int result = SubscriptionNative.dxfg_DXFeedSubscription_addChangeListener(
                        thread,
                        dxfgSubscription,
                        dxfgFeedEventListener);

        return OlderEntryPoint.failed(stored, result == ExceptionHandlerReturnMinusOne.EXECUTE_FAIL)
                ? ExceptionHandlerReturnMinusOne.EXECUTE_FAIL
                : result;
    }

    @CEntryPoint(
            name = "dxfg_v4_DXFeedSubscription_removeChangeListener",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_DXFeedSubscription_removeChangeListener(
            final IsolateThread thread,
            final DxfgSubscription<DXFeedSubscription<EventType<?>>> dxfgSubscription,
            final DxfgObservableSubscriptionChangeListener dxfgFeedEventListener
    ) {
        final Throwable stored = OlderEntryPoint.takeStoredException();
        final int result = SubscriptionNative.dxfg_DXFeedSubscription_removeChangeListener(
                        thread,
                        dxfgSubscription,
                        dxfgFeedEventListener);

        return OlderEntryPoint.failed(stored, result == ExceptionHandlerReturnMinusOne.EXECUTE_FAIL)
                ? ExceptionHandlerReturnMinusOne.EXECUTE_FAIL
                : result;
    }

    @CEntryPoint(
            name = "dxfg_v4_DXFeedSubscription_getEventsBatchLimit",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_DXFeedSubscription_getEventsBatchLimit(
            final IsolateThread thread,
            final DxfgSubscription<DXFeedSubscription<EventType<?>>> dxfgSubscription,
            @DxfgOut final CInt32Pointer eventsBatchLimit
    ) {
        if (eventsBatchLimit.isNull()) {
            throw new IllegalArgumentException("The `eventsBatchLimit` pointer is null");
        }

        final Throwable stored = OlderEntryPoint.takeStoredException();
        final int result = SubscriptionNative.dxfg_DXFeedSubscription_getEventsBatchLimit(thread, dxfgSubscription);

        if (OlderEntryPoint.failed(stored, result == ExceptionHandlerReturnMinusOne.EXECUTE_FAIL)) {
            return ExceptionHandlerReturnMinusOne.EXECUTE_FAIL;
        }

        eventsBatchLimit.write(result);

        return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
    }

    @CEntryPoint(
            name = "dxfg_v4_DXFeedSubscription_setEventsBatchLimit",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_DXFeedSubscription_setEventsBatchLimit(
            final IsolateThread thread,
            final DxfgSubscription<DXFeedSubscription<EventType<?>>> dxfgSubscription,
            final int eventsBatchLimit
    ) {
        final Throwable stored = OlderEntryPoint.takeStoredException();
        final int result = SubscriptionNative.dxfg_DXFeedSubscription_setEventsBatchLimit(
                        thread,
                        dxfgSubscription,
                        eventsBatchLimit);

        return OlderEntryPoint.failed(stored, result == ExceptionHandlerReturnMinusOne.EXECUTE_FAIL)
                ? ExceptionHandlerReturnMinusOne.EXECUTE_FAIL
                : result;
    }

    @CEntryPoint(
            name = "dxfg_v4_ObservableSubscriptionChangeListener_new",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_ObservableSubscriptionChangeListener_new(
            final IsolateThread thread,
            final DxfgObservableSubscriptionChangeListenerFunctionSymbolsAdded functionSymbolsAdded,
            final DxfgObservableSubscriptionChangeListenerFunctionSymbolsRemoved functionSymbolsRemoved,
            final DxfgObservableSubscriptionChangeListenerFunctionSubscriptionClosed functionSubscriptionClosed,
            final VoidPointer userData,
            @DxfgOut final WordPointer listener
    ) {
        if (listener.isNull()) {
            throw new IllegalArgumentException("The `listener` pointer is null");
        }

        final Throwable stored = OlderEntryPoint.takeStoredException();
        final DxfgObservableSubscriptionChangeListener result = SubscriptionNative.dxfg_ObservableSubscriptionChangeListener_new(
                        thread,
                        functionSymbolsAdded,
                        functionSymbolsRemoved,
                        functionSubscriptionClosed,
                        userData);

        if (OlderEntryPoint.failed(stored, result.isNull())) {
            return ExceptionHandlerReturnMinusOne.EXECUTE_FAIL;
        }

        listener.write(result);

        return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
    }
}
