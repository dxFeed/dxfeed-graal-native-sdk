// Copyright (c) 2026 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

package com.dxfeed.sdk.indexedeventtxmodel;

import com.dxfeed.sdk.common.CInt32Pointer;
import com.dxfeed.sdk.common.DxfgOut;
import com.dxfeed.sdk.events.DxfgEventClazz;
import com.dxfeed.sdk.exception.ExceptionHandlerReturnMinusOne;
import com.dxfeed.sdk.exception.OlderEntryPoint;
import com.dxfeed.sdk.feed.DxfgFeed;
import com.dxfeed.sdk.javac.DxfgExecutorHandle;
import com.dxfeed.sdk.javac.DxfgTimePeriodHandle;
import com.dxfeed.sdk.source.DxfgIndexedEventSourcePointer;
import com.dxfeed.sdk.source.DxfgIndexedEventSourcePointerPointer;
import com.dxfeed.sdk.subscriptioncontroller.DxfgSubscriptionControllerHandlePointer;
import com.dxfeed.sdk.symbol.DxfgSymbol;
import com.dxfeed.sdk.symbol.DxfgSymbolPointer;
import org.graalvm.nativeimage.IsolateThread;
import org.graalvm.nativeimage.c.CContext;
import org.graalvm.nativeimage.c.function.CEntryPoint;
import org.graalvm.nativeimage.c.type.CCharPointer;
import org.graalvm.nativeimage.c.type.CCharPointerPointer;
import org.graalvm.nativeimage.c.type.CConst;
import org.graalvm.nativeimage.c.type.VoidPointer;

/**
 * The v4 twins of the functions of {@link IndexedEventTxModelNative}: each one calls the old function as a Java method
 * ({@link OlderEntryPoint} tells whether it failed) and returns its result through an out parameter (see
 * dxfg_v4_api.h).
 */
@CContext(IndexedEventTxModelDirectives.class)
public final class IndexedEventTxModelV4Native {

    private IndexedEventTxModelV4Native() {
    }

    @CEntryPoint(
            name = "dxfg_v4_IndexedEventTxModel_newBuilder",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_IndexedEventTxModel_newBuilder(
            final IsolateThread thread,
            final DxfgEventClazz eventType,
            @DxfgOut final DxfgIndexedEventTxModelBuilderHandlePointer builder
    ) {
        final Throwable stored = OlderEntryPoint.takeStoredException();
        final int result = IndexedEventTxModelNative.dxfg_IndexedEventTxModel_newBuilder(thread, eventType, builder);

        return OlderEntryPoint.failed(stored, result == ExceptionHandlerReturnMinusOne.EXECUTE_FAIL)
                ? ExceptionHandlerReturnMinusOne.EXECUTE_FAIL
                : result;
    }

    @CEntryPoint(
            name = "dxfg_v4_IndexedEventTxModel_getIndexedEventSubscriptionSymbol",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_IndexedEventTxModel_getIndexedEventSubscriptionSymbol(
            final IsolateThread thread,
            final DxfgIndexedEventTxModelHandle model,
            @DxfgOut final DxfgSymbolPointer symbol
    ) {
        final Throwable stored = OlderEntryPoint.takeStoredException();
        final int result = IndexedEventTxModelNative.dxfg_IndexedEventTxModel_getIndexedEventSubscriptionSymbol(
                        thread,
                        model,
                        symbol);

        return OlderEntryPoint.failed(stored, result == ExceptionHandlerReturnMinusOne.EXECUTE_FAIL)
                ? ExceptionHandlerReturnMinusOne.EXECUTE_FAIL
                : result;
    }

    @CEntryPoint(
            name = "dxfg_v4_IndexedEventTxModel_getSymbol",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_IndexedEventTxModel_getSymbol(
            final IsolateThread thread,
            final DxfgIndexedEventTxModelHandle model,
            @DxfgOut final CCharPointerPointer symbol
    ) {
        final Throwable stored = OlderEntryPoint.takeStoredException();
        final int result = IndexedEventTxModelNative.dxfg_IndexedEventTxModel_getSymbol(thread, model, symbol);

        return OlderEntryPoint.failed(stored, result == ExceptionHandlerReturnMinusOne.EXECUTE_FAIL)
                ? ExceptionHandlerReturnMinusOne.EXECUTE_FAIL
                : result;
    }

    @CEntryPoint(
            name = "dxfg_v4_IndexedEventTxModel_getSource",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_IndexedEventTxModel_getSource(
            final IsolateThread thread,
            final DxfgIndexedEventTxModelHandle model,
            @DxfgOut final DxfgIndexedEventSourcePointerPointer source
    ) {
        final Throwable stored = OlderEntryPoint.takeStoredException();
        final int result = IndexedEventTxModelNative.dxfg_IndexedEventTxModel_getSource(thread, model, source);

        return OlderEntryPoint.failed(stored, result == ExceptionHandlerReturnMinusOne.EXECUTE_FAIL)
                ? ExceptionHandlerReturnMinusOne.EXECUTE_FAIL
                : result;
    }

    @CEntryPoint(
            name = "dxfg_v4_IndexedEventTxModel_getSubscriptionController",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_IndexedEventTxModel_getSubscriptionController(
            final IsolateThread thread,
            final DxfgIndexedEventTxModelHandle model,
            @DxfgOut final DxfgSubscriptionControllerHandlePointer subscriptionController
    ) {
        final Throwable stored = OlderEntryPoint.takeStoredException();
        final int result = IndexedEventTxModelNative.dxfg_IndexedEventTxModel_getSubscriptionController(
                        thread,
                        model,
                        subscriptionController);

        return OlderEntryPoint.failed(stored, result == ExceptionHandlerReturnMinusOne.EXECUTE_FAIL)
                ? ExceptionHandlerReturnMinusOne.EXECUTE_FAIL
                : result;
    }

    @CEntryPoint(
            name = "dxfg_v4_IndexedEventTxModel_isClosed",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_IndexedEventTxModel_isClosed(
            final IsolateThread thread,
            final DxfgIndexedEventTxModelHandle model,
            @DxfgOut final CInt32Pointer isClosed
    ) {
        final Throwable stored = OlderEntryPoint.takeStoredException();
        final int result = IndexedEventTxModelNative.dxfg_IndexedEventTxModel_isClosed(thread, model, isClosed);

        return OlderEntryPoint.failed(stored, result == ExceptionHandlerReturnMinusOne.EXECUTE_FAIL)
                ? ExceptionHandlerReturnMinusOne.EXECUTE_FAIL
                : result;
    }

    @CEntryPoint(
            name = "dxfg_v4_IndexedEventTxModel_close",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_IndexedEventTxModel_close(
            final IsolateThread thread,
            final DxfgIndexedEventTxModelHandle model
    ) {
        final Throwable stored = OlderEntryPoint.takeStoredException();
        final int result = IndexedEventTxModelNative.dxfg_IndexedEventTxModel_close(thread, model);

        return OlderEntryPoint.failed(stored, result == ExceptionHandlerReturnMinusOne.EXECUTE_FAIL)
                ? ExceptionHandlerReturnMinusOne.EXECUTE_FAIL
                : result;
    }

    @CEntryPoint(
            name = "dxfg_v4_IndexedEventTxModel_Builder_new",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_IndexedEventTxModel_Builder_new(
            final IsolateThread thread,
            final DxfgEventClazz eventType,
            @DxfgOut final DxfgIndexedEventTxModelBuilderHandlePointer builder
    ) {
        final Throwable stored = OlderEntryPoint.takeStoredException();
        final int result = IndexedEventTxModelNative.dxfg_IndexedEventTxModel_Builder_new(thread, eventType, builder);

        return OlderEntryPoint.failed(stored, result == ExceptionHandlerReturnMinusOne.EXECUTE_FAIL)
                ? ExceptionHandlerReturnMinusOne.EXECUTE_FAIL
                : result;
    }

    @CEntryPoint(
            name = "dxfg_v4_IndexedEventTxModel_Builder_withFeed",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_IndexedEventTxModel_Builder_withFeed(
            final IsolateThread thread,
            final DxfgIndexedEventTxModelBuilderHandle builder,
            final DxfgFeed feed
    ) {
        final Throwable stored = OlderEntryPoint.takeStoredException();
        final int result = IndexedEventTxModelNative.dxfg_IndexedEventTxModel_Builder_withFeed(thread, builder, feed);

        return OlderEntryPoint.failed(stored, result == ExceptionHandlerReturnMinusOne.EXECUTE_FAIL)
                ? ExceptionHandlerReturnMinusOne.EXECUTE_FAIL
                : result;
    }

    @CEntryPoint(
            name = "dxfg_v4_IndexedEventTxModel_Builder_withSymbol",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_IndexedEventTxModel_Builder_withSymbol(
            final IsolateThread thread,
            final DxfgIndexedEventTxModelBuilderHandle builder,
            final DxfgSymbol symbol
    ) {
        final Throwable stored = OlderEntryPoint.takeStoredException();
        final int result = IndexedEventTxModelNative.dxfg_IndexedEventTxModel_Builder_withSymbol(
                        thread,
                        builder,
                        symbol);

        return OlderEntryPoint.failed(stored, result == ExceptionHandlerReturnMinusOne.EXECUTE_FAIL)
                ? ExceptionHandlerReturnMinusOne.EXECUTE_FAIL
                : result;
    }

    @CEntryPoint(
            name = "dxfg_v4_IndexedEventTxModel_Builder_withStringSymbol",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_IndexedEventTxModel_Builder_withStringSymbol(
            final IsolateThread thread,
            final DxfgIndexedEventTxModelBuilderHandle builder,
            @CConst final CCharPointer symbol
    ) {
        final Throwable stored = OlderEntryPoint.takeStoredException();
        final int result = IndexedEventTxModelNative.dxfg_IndexedEventTxModel_Builder_withSymbol(
                        thread,
                        builder,
                        symbol);

        return OlderEntryPoint.failed(stored, result == ExceptionHandlerReturnMinusOne.EXECUTE_FAIL)
                ? ExceptionHandlerReturnMinusOne.EXECUTE_FAIL
                : result;
    }

    @CEntryPoint(
            name = "dxfg_v4_IndexedEventTxModel_Builder_withSource",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_IndexedEventTxModel_Builder_withSource(
            final IsolateThread thread,
            final DxfgIndexedEventTxModelBuilderHandle builder,
            final DxfgIndexedEventSourcePointer source
    ) {
        final Throwable stored = OlderEntryPoint.takeStoredException();
        final int result = IndexedEventTxModelNative.dxfg_IndexedEventTxModel_Builder_withSource(
                        thread,
                        builder,
                        source);

        return OlderEntryPoint.failed(stored, result == ExceptionHandlerReturnMinusOne.EXECUTE_FAIL)
                ? ExceptionHandlerReturnMinusOne.EXECUTE_FAIL
                : result;
    }

    @CEntryPoint(
            name = "dxfg_v4_IndexedEventTxModel_Listener_new",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_IndexedEventTxModel_Listener_new(
            final IsolateThread thread,
            final DxfgIndexedEventTxModelListenerEventsReceivedFunctionPointer eventsReceived,
            final VoidPointer userData,
            @DxfgOut final DxfgIndexedEventTxModelListenerHandlePointer listener
    ) {
        final Throwable stored = OlderEntryPoint.takeStoredException();
        final int result = IndexedEventTxModelNative.dxfg_IndexedEventTxModel_Listener_new(
                        thread,
                        eventsReceived,
                        userData,
                        listener);

        return OlderEntryPoint.failed(stored, result == ExceptionHandlerReturnMinusOne.EXECUTE_FAIL)
                ? ExceptionHandlerReturnMinusOne.EXECUTE_FAIL
                : result;
    }

    @CEntryPoint(
            name = "dxfg_v4_IndexedEventTxModel_Builder_withListener",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_IndexedEventTxModel_Builder_withListener(
            final IsolateThread thread,
            final DxfgIndexedEventTxModelBuilderHandle builder,
            final DxfgIndexedEventTxModelListenerHandle listener
    ) {
        final Throwable stored = OlderEntryPoint.takeStoredException();
        final int result = IndexedEventTxModelNative.dxfg_IndexedEventTxModel_Builder_withListener(
                        thread,
                        builder,
                        listener);

        return OlderEntryPoint.failed(stored, result == ExceptionHandlerReturnMinusOne.EXECUTE_FAIL)
                ? ExceptionHandlerReturnMinusOne.EXECUTE_FAIL
                : result;
    }

    @CEntryPoint(
            name = "dxfg_v4_IndexedEventTxModel_Builder_withEventsBatchLimit",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_IndexedEventTxModel_Builder_withEventsBatchLimit(
            final IsolateThread thread,
            final DxfgIndexedEventTxModelBuilderHandle builder,
            final int eventsBatchLimit
    ) {
        final Throwable stored = OlderEntryPoint.takeStoredException();
        final int result = IndexedEventTxModelNative.dxfg_IndexedEventTxModel_Builder_withEventsBatchLimit(
                        thread,
                        builder,
                        eventsBatchLimit);

        return OlderEntryPoint.failed(stored, result == ExceptionHandlerReturnMinusOne.EXECUTE_FAIL)
                ? ExceptionHandlerReturnMinusOne.EXECUTE_FAIL
                : result;
    }

    @CEntryPoint(
            name = "dxfg_v4_IndexedEventTxModel_Builder_withAggregationPeriod",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_IndexedEventTxModel_Builder_withAggregationPeriod(
            final IsolateThread thread,
            final DxfgIndexedEventTxModelBuilderHandle builder,
            final DxfgTimePeriodHandle aggregationPeriod
    ) {
        final Throwable stored = OlderEntryPoint.takeStoredException();
        final int result = IndexedEventTxModelNative.dxfg_IndexedEventTxModel_Builder_withAggregationPeriod(
                        thread,
                        builder,
                        aggregationPeriod);

        return OlderEntryPoint.failed(stored, result == ExceptionHandlerReturnMinusOne.EXECUTE_FAIL)
                ? ExceptionHandlerReturnMinusOne.EXECUTE_FAIL
                : result;
    }

    @CEntryPoint(
            name = "dxfg_v4_IndexedEventTxModel_Builder_withAggregationPeriodMillis",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_IndexedEventTxModel_Builder_withAggregationPeriodMillis(
            final IsolateThread thread,
            final DxfgIndexedEventTxModelBuilderHandle builder,
            final long aggregationPeriod
    ) {
        final Throwable stored = OlderEntryPoint.takeStoredException();
        final int result = IndexedEventTxModelNative.dxfg_IndexedEventTxModel_Builder_withAggregationPeriod(
                        thread,
                        builder,
                        aggregationPeriod);

        return OlderEntryPoint.failed(stored, result == ExceptionHandlerReturnMinusOne.EXECUTE_FAIL)
                ? ExceptionHandlerReturnMinusOne.EXECUTE_FAIL
                : result;
    }

    @CEntryPoint(
            name = "dxfg_v4_IndexedEventTxModel_Builder_withExecutor",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_IndexedEventTxModel_Builder_withExecutor(
            final IsolateThread thread,
            final DxfgIndexedEventTxModelBuilderHandle builder,
            final DxfgExecutorHandle executor
    ) {
        final Throwable stored = OlderEntryPoint.takeStoredException();
        final int result = IndexedEventTxModelNative.dxfg_IndexedEventTxModel_Builder_withExecutor(
                        thread,
                        builder,
                        executor);

        return OlderEntryPoint.failed(stored, result == ExceptionHandlerReturnMinusOne.EXECUTE_FAIL)
                ? ExceptionHandlerReturnMinusOne.EXECUTE_FAIL
                : result;
    }

    @CEntryPoint(
            name = "dxfg_v4_IndexedEventTxModel_Builder_build",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_IndexedEventTxModel_Builder_build(
            final IsolateThread thread,
            final DxfgIndexedEventTxModelBuilderHandle builder,
            @DxfgOut final DxfgIndexedEventTxModelHandlePointer model
    ) {
        final Throwable stored = OlderEntryPoint.takeStoredException();
        final int result = IndexedEventTxModelNative.dxfg_IndexedEventTxModel_Builder_build(thread, builder, model);

        return OlderEntryPoint.failed(stored, result == ExceptionHandlerReturnMinusOne.EXECUTE_FAIL)
                ? ExceptionHandlerReturnMinusOne.EXECUTE_FAIL
                : result;
    }
}
