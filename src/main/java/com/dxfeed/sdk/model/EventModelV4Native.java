// Copyright (c) 2026 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

package com.dxfeed.sdk.model;

import com.dxfeed.sdk.common.CInt32Pointer;
import com.dxfeed.sdk.common.DxfgOut;
import com.dxfeed.sdk.events.DxfgEventClazz;
import com.dxfeed.sdk.events.DxfgEventTypeListPointer;
import com.dxfeed.sdk.exception.ExceptionHandlerReturnMinusOne;
import com.dxfeed.sdk.exception.OlderEntryPoint;
import com.dxfeed.sdk.feed.DxfgFeed;
import com.dxfeed.sdk.javac.DxfgExecutorHandle;
import com.dxfeed.sdk.symbol.DxfgSymbol;
import org.graalvm.nativeimage.IsolateThread;
import org.graalvm.nativeimage.c.CContext;
import org.graalvm.nativeimage.c.function.CEntryPoint;
import org.graalvm.nativeimage.c.type.CCharPointer;
import org.graalvm.nativeimage.c.type.CLongPointer;
import org.graalvm.nativeimage.c.type.VoidPointer;
import org.graalvm.nativeimage.c.type.WordPointer;

/**
 * The v4 twins of the functions of {@link EventModelNative}: each one calls the old function as a Java method
 * ({@link OlderEntryPoint} tells whether it failed) and returns its result through an out parameter (see
 * dxfg_v4_api.h).
 */
@CContext(ModelDirectives.class)
public final class EventModelV4Native {

    private EventModelV4Native() {
    }

    @CEntryPoint(
            name = "dxfg_v4_OrderBookModel_new",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_OrderBookModel_new(
            final IsolateThread thread,
            @DxfgOut final WordPointer orderBookModel
    ) {
        if (orderBookModel.isNull()) {
            throw new IllegalArgumentException("The `orderBookModel` pointer is null");
        }

        final Throwable stored = OlderEntryPoint.takeStoredException();
        final DxfgOrderBookModel result = EventModelNative.dxfg_OrderBookModel_new(thread);

        if (OlderEntryPoint.failed(stored, result.isNull())) {
            return ExceptionHandlerReturnMinusOne.EXECUTE_FAIL;
        }

        orderBookModel.write(result);

        return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
    }

    @CEntryPoint(
            name = "dxfg_v4_IndexedEventModel_new",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_IndexedEventModel_new(
            final IsolateThread thread,
            final DxfgEventClazz clazz,
            @DxfgOut final WordPointer indexedEventModel
    ) {
        if (indexedEventModel.isNull()) {
            throw new IllegalArgumentException("The `indexedEventModel` pointer is null");
        }

        final Throwable stored = OlderEntryPoint.takeStoredException();
        final DxfgIndexedEventModel result = EventModelNative.dxfg_IndexedEventModel_new(thread, clazz);

        if (OlderEntryPoint.failed(stored, result.isNull())) {
            return ExceptionHandlerReturnMinusOne.EXECUTE_FAIL;
        }

        indexedEventModel.write(result);

        return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
    }

    @CEntryPoint(
            name = "dxfg_v4_TimeSeriesEventModel_new",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_TimeSeriesEventModel_new(
            final IsolateThread thread,
            final DxfgEventClazz clazz,
            @DxfgOut final WordPointer timeSeriesEventModel
    ) {
        if (timeSeriesEventModel.isNull()) {
            throw new IllegalArgumentException("The `timeSeriesEventModel` pointer is null");
        }

        final Throwable stored = OlderEntryPoint.takeStoredException();
        final DxfgTimeSeriesEventModel result = EventModelNative.dxfg_TimeSeriesEventModel_new(thread, clazz);

        if (OlderEntryPoint.failed(stored, result.isNull())) {
            return ExceptionHandlerReturnMinusOne.EXECUTE_FAIL;
        }

        timeSeriesEventModel.write(result);

        return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
    }

    @CEntryPoint(
            name = "dxfg_v4_OrderBookModel_attach",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_OrderBookModel_attach(
            final IsolateThread thread,
            final DxfgOrderBookModel dxfgOrderBookModel,
            final DxfgFeed feed
    ) {
        final Throwable stored = OlderEntryPoint.takeStoredException();
        final int result = EventModelNative.dxfg_OrderBookModel_attach(thread, dxfgOrderBookModel, feed);

        return OlderEntryPoint.failed(stored, result == ExceptionHandlerReturnMinusOne.EXECUTE_FAIL)
                ? ExceptionHandlerReturnMinusOne.EXECUTE_FAIL
                : result;
    }

    @CEntryPoint(
            name = "dxfg_v4_OrderBookModel_detach",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_OrderBookModel_detach(
            final IsolateThread thread,
            final DxfgOrderBookModel dxfgOrderBookModel,
            final DxfgFeed feed
    ) {
        final Throwable stored = OlderEntryPoint.takeStoredException();
        final int result = EventModelNative.dxfg_OrderBookModel_detach(thread, dxfgOrderBookModel, feed);

        return OlderEntryPoint.failed(stored, result == ExceptionHandlerReturnMinusOne.EXECUTE_FAIL)
                ? ExceptionHandlerReturnMinusOne.EXECUTE_FAIL
                : result;
    }

    @CEntryPoint(
            name = "dxfg_v4_OrderBookModel_close",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_OrderBookModel_close(
            final IsolateThread thread,
            final DxfgOrderBookModel dxfgOrderBookModel
    ) {
        final Throwable stored = OlderEntryPoint.takeStoredException();
        final int result = EventModelNative.dxfg_OrderBookModel_close(thread, dxfgOrderBookModel);

        return OlderEntryPoint.failed(stored, result == ExceptionHandlerReturnMinusOne.EXECUTE_FAIL)
                ? ExceptionHandlerReturnMinusOne.EXECUTE_FAIL
                : result;
    }

    @CEntryPoint(
            name = "dxfg_v4_OrderBookModel_getExecutor",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_OrderBookModel_getExecutor(
            final IsolateThread thread,
            final DxfgOrderBookModel dxfgOrderBookModel,
            @DxfgOut final WordPointer executor
    ) {
        if (executor.isNull()) {
            throw new IllegalArgumentException("The `executor` pointer is null");
        }

        final Throwable stored = OlderEntryPoint.takeStoredException();
        final DxfgExecutorHandle result = EventModelNative.dxfg_OrderBookModel_getExecutor(thread, dxfgOrderBookModel);

        if (OlderEntryPoint.failed(stored, result.isNull())) {
            return ExceptionHandlerReturnMinusOne.EXECUTE_FAIL;
        }

        executor.write(result);

        return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
    }

    @CEntryPoint(
            name = "dxfg_v4_OrderBookModel_setExecutor",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_OrderBookModel_setExecutor(
            final IsolateThread thread,
            final DxfgOrderBookModel dxfgOrderBookModel,
            final DxfgExecutorHandle executor
    ) {
        final Throwable stored = OlderEntryPoint.takeStoredException();
        final int result = EventModelNative.dxfg_OrderBookModel_setExecutor(thread, dxfgOrderBookModel, executor);

        return OlderEntryPoint.failed(stored, result == ExceptionHandlerReturnMinusOne.EXECUTE_FAIL)
                ? ExceptionHandlerReturnMinusOne.EXECUTE_FAIL
                : result;
    }

    @CEntryPoint(
            name = "dxfg_v4_OrderBookModel_clear",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_OrderBookModel_clear(
            final IsolateThread thread,
            final DxfgOrderBookModel dxfgOrderBookModel
    ) {
        final Throwable stored = OlderEntryPoint.takeStoredException();
        final int result = EventModelNative.dxfg_OrderBookModel_clear(thread, dxfgOrderBookModel);

        return OlderEntryPoint.failed(stored, result == ExceptionHandlerReturnMinusOne.EXECUTE_FAIL)
                ? ExceptionHandlerReturnMinusOne.EXECUTE_FAIL
                : result;
    }

    @CEntryPoint(
            name = "dxfg_v4_IndexedEventModel_attach",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_IndexedEventModel_attach(
            final IsolateThread thread,
            final DxfgIndexedEventModel dxfgIndexedEventModel,
            final DxfgFeed feed
    ) {
        final Throwable stored = OlderEntryPoint.takeStoredException();
        final int result = EventModelNative.dxfg_IndexedEventModel_attach(thread, dxfgIndexedEventModel, feed);

        return OlderEntryPoint.failed(stored, result == ExceptionHandlerReturnMinusOne.EXECUTE_FAIL)
                ? ExceptionHandlerReturnMinusOne.EXECUTE_FAIL
                : result;
    }

    @CEntryPoint(
            name = "dxfg_v4_IndexedEventModel_detach",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_IndexedEventModel_detach(
            final IsolateThread thread,
            final DxfgIndexedEventModel dxfgIndexedEventModel,
            final DxfgFeed feed
    ) {
        final Throwable stored = OlderEntryPoint.takeStoredException();
        final int result = EventModelNative.dxfg_IndexedEventModel_detach(thread, dxfgIndexedEventModel, feed);

        return OlderEntryPoint.failed(stored, result == ExceptionHandlerReturnMinusOne.EXECUTE_FAIL)
                ? ExceptionHandlerReturnMinusOne.EXECUTE_FAIL
                : result;
    }

    @CEntryPoint(
            name = "dxfg_v4_IndexedEventModel_close",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_IndexedEventModel_close(
            final IsolateThread thread,
            final DxfgIndexedEventModel dxfgIndexedEventModel
    ) {
        final Throwable stored = OlderEntryPoint.takeStoredException();
        final int result = EventModelNative.dxfg_IndexedEventModel_close(thread, dxfgIndexedEventModel);

        return OlderEntryPoint.failed(stored, result == ExceptionHandlerReturnMinusOne.EXECUTE_FAIL)
                ? ExceptionHandlerReturnMinusOne.EXECUTE_FAIL
                : result;
    }

    @CEntryPoint(
            name = "dxfg_v4_IndexedEventModel_getExecutor",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_IndexedEventModel_getExecutor(
            final IsolateThread thread,
            final DxfgIndexedEventModel dxfgIndexedEventModel,
            @DxfgOut final WordPointer executor
    ) {
        if (executor.isNull()) {
            throw new IllegalArgumentException("The `executor` pointer is null");
        }

        final Throwable stored = OlderEntryPoint.takeStoredException();
        final DxfgExecutorHandle result = EventModelNative.dxfg_IndexedEventModel_getExecutor(thread, dxfgIndexedEventModel);

        if (OlderEntryPoint.failed(stored, result.isNull())) {
            return ExceptionHandlerReturnMinusOne.EXECUTE_FAIL;
        }

        executor.write(result);

        return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
    }

    @CEntryPoint(
            name = "dxfg_v4_IndexedEventModel_setExecutor",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_IndexedEventModel_setExecutor(
            final IsolateThread thread,
            final DxfgIndexedEventModel dxfgIndexedEventModel,
            final DxfgExecutorHandle executor
    ) {
        final Throwable stored = OlderEntryPoint.takeStoredException();
        final int result = EventModelNative.dxfg_IndexedEventModel_setExecutor(
                        thread,
                        dxfgIndexedEventModel,
                        executor);

        return OlderEntryPoint.failed(stored, result == ExceptionHandlerReturnMinusOne.EXECUTE_FAIL)
                ? ExceptionHandlerReturnMinusOne.EXECUTE_FAIL
                : result;
    }

    @CEntryPoint(
            name = "dxfg_v4_IndexedEventModel_clear",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_IndexedEventModel_clear(
            final IsolateThread thread,
            final DxfgIndexedEventModel dxfgIndexedEventModel
    ) {
        final Throwable stored = OlderEntryPoint.takeStoredException();
        final int result = EventModelNative.dxfg_IndexedEventModel_clear(thread, dxfgIndexedEventModel);

        return OlderEntryPoint.failed(stored, result == ExceptionHandlerReturnMinusOne.EXECUTE_FAIL)
                ? ExceptionHandlerReturnMinusOne.EXECUTE_FAIL
                : result;
    }

    @CEntryPoint(
            name = "dxfg_v4_TimeSeriesEventModel_attach",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_TimeSeriesEventModel_attach(
            final IsolateThread thread,
            final DxfgTimeSeriesEventModel dxfgTimeSeriesEventModel,
            final DxfgFeed feed
    ) {
        final Throwable stored = OlderEntryPoint.takeStoredException();
        final int result = EventModelNative.dxfg_TimeSeriesEventModel_attach(thread, dxfgTimeSeriesEventModel, feed);

        return OlderEntryPoint.failed(stored, result == ExceptionHandlerReturnMinusOne.EXECUTE_FAIL)
                ? ExceptionHandlerReturnMinusOne.EXECUTE_FAIL
                : result;
    }

    @CEntryPoint(
            name = "dxfg_v4_TimeSeriesEventModel_detach",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_TimeSeriesEventModel_detach(
            final IsolateThread thread,
            final DxfgTimeSeriesEventModel dxfgTimeSeriesEventModel,
            final DxfgFeed feed
    ) {
        final Throwable stored = OlderEntryPoint.takeStoredException();
        final int result = EventModelNative.dxfg_TimeSeriesEventModel_detach(thread, dxfgTimeSeriesEventModel, feed);

        return OlderEntryPoint.failed(stored, result == ExceptionHandlerReturnMinusOne.EXECUTE_FAIL)
                ? ExceptionHandlerReturnMinusOne.EXECUTE_FAIL
                : result;
    }

    @CEntryPoint(
            name = "dxfg_v4_TimeSeriesEventModel_close",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_TimeSeriesEventModel_close(
            final IsolateThread thread,
            final DxfgTimeSeriesEventModel dxfgTimeSeriesEventModel
    ) {
        final Throwable stored = OlderEntryPoint.takeStoredException();
        final int result = EventModelNative.dxfg_TimeSeriesEventModel_close(thread, dxfgTimeSeriesEventModel);

        return OlderEntryPoint.failed(stored, result == ExceptionHandlerReturnMinusOne.EXECUTE_FAIL)
                ? ExceptionHandlerReturnMinusOne.EXECUTE_FAIL
                : result;
    }

    @CEntryPoint(
            name = "dxfg_v4_TimeSeriesEventModel_getExecutor",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_TimeSeriesEventModel_getExecutor(
            final IsolateThread thread,
            final DxfgTimeSeriesEventModel dxfgTimeSeriesEventModel,
            @DxfgOut final WordPointer executor
    ) {
        if (executor.isNull()) {
            throw new IllegalArgumentException("The `executor` pointer is null");
        }

        final Throwable stored = OlderEntryPoint.takeStoredException();
        final DxfgExecutorHandle result = EventModelNative.dxfg_TimeSeriesEventModel_getExecutor(thread, dxfgTimeSeriesEventModel);

        if (OlderEntryPoint.failed(stored, result.isNull())) {
            return ExceptionHandlerReturnMinusOne.EXECUTE_FAIL;
        }

        executor.write(result);

        return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
    }

    @CEntryPoint(
            name = "dxfg_v4_TimeSeriesEventModel_setExecutor",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_TimeSeriesEventModel_setExecutor(
            final IsolateThread thread,
            final DxfgTimeSeriesEventModel dxfgTimeSeriesEventModel,
            final DxfgExecutorHandle executor
    ) {
        final Throwable stored = OlderEntryPoint.takeStoredException();
        final int result = EventModelNative.dxfg_TimeSeriesEventModel_setExecutor(
                        thread,
                        dxfgTimeSeriesEventModel,
                        executor);

        return OlderEntryPoint.failed(stored, result == ExceptionHandlerReturnMinusOne.EXECUTE_FAIL)
                ? ExceptionHandlerReturnMinusOne.EXECUTE_FAIL
                : result;
    }

    @CEntryPoint(
            name = "dxfg_v4_TimeSeriesEventModel_clear",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_TimeSeriesEventModel_clear(
            final IsolateThread thread,
            final DxfgTimeSeriesEventModel dxfgTimeSeriesEventModel
    ) {
        final Throwable stored = OlderEntryPoint.takeStoredException();
        final int result = EventModelNative.dxfg_TimeSeriesEventModel_clear(thread, dxfgTimeSeriesEventModel);

        return OlderEntryPoint.failed(stored, result == ExceptionHandlerReturnMinusOne.EXECUTE_FAIL)
                ? ExceptionHandlerReturnMinusOne.EXECUTE_FAIL
                : result;
    }

    @CEntryPoint(
            name = "dxfg_v4_OrderBookModel_getFilter",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_OrderBookModel_getFilter(
            final IsolateThread thread,
            final DxfgOrderBookModel dxfgOrderBookModel,
            @DxfgOut final CInt32Pointer filter
    ) {
        if (filter.isNull()) {
            throw new IllegalArgumentException("The `filter` pointer is null");
        }

        final Throwable stored = OlderEntryPoint.takeStoredException();
        final int result = EventModelNative.dxfg_OrderBookModel_getFilter(thread, dxfgOrderBookModel);

        if (OlderEntryPoint.failed(stored, result == ExceptionHandlerReturnMinusOne.EXECUTE_FAIL)) {
            return ExceptionHandlerReturnMinusOne.EXECUTE_FAIL;
        }

        filter.write(result);

        return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
    }

    @CEntryPoint(
            name = "dxfg_v4_OrderBookModel_setFilter",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_OrderBookModel_setFilter(
            final IsolateThread thread,
            final DxfgOrderBookModel dxfgOrderBookModel,
            final DxfgOrderBookModelFilter dxfgOrderBookModelFilter
    ) {
        final Throwable stored = OlderEntryPoint.takeStoredException();
        final int result = EventModelNative.dxfg_OrderBookModel_setFilter(
                        thread,
                        dxfgOrderBookModel,
                        dxfgOrderBookModelFilter);

        return OlderEntryPoint.failed(stored, result == ExceptionHandlerReturnMinusOne.EXECUTE_FAIL)
                ? ExceptionHandlerReturnMinusOne.EXECUTE_FAIL
                : result;
    }

    @CEntryPoint(
            name = "dxfg_v4_OrderBookModel_getSymbol",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_OrderBookModel_getSymbol(
            final IsolateThread thread,
            final DxfgOrderBookModel dxfgOrderBookModel,
            @DxfgOut final WordPointer symbol
    ) {
        if (symbol.isNull()) {
            throw new IllegalArgumentException("The `symbol` pointer is null");
        }

        final Throwable stored = OlderEntryPoint.takeStoredException();
        final CCharPointer result = EventModelNative.dxfg_OrderBookModel_getSymbol(thread, dxfgOrderBookModel);

        if (OlderEntryPoint.failed(stored, result.isNull())) {
            return ExceptionHandlerReturnMinusOne.EXECUTE_FAIL;
        }

        symbol.write(result);

        return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
    }

    @CEntryPoint(
            name = "dxfg_v4_OrderBookModel_setSymbol",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_OrderBookModel_setSymbol(
            final IsolateThread thread,
            final DxfgOrderBookModel dxfgOrderBookModel,
            final CCharPointer symbol
    ) {
        final Throwable stored = OlderEntryPoint.takeStoredException();
        final int result = EventModelNative.dxfg_OrderBookModel_setSymbol(thread, dxfgOrderBookModel, symbol);

        return OlderEntryPoint.failed(stored, result == ExceptionHandlerReturnMinusOne.EXECUTE_FAIL)
                ? ExceptionHandlerReturnMinusOne.EXECUTE_FAIL
                : result;
    }

    @CEntryPoint(
            name = "dxfg_v4_IndexedEventModel_getSymbol",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_IndexedEventModel_getSymbol(
            final IsolateThread thread,
            final DxfgIndexedEventModel dxfgIndexedEventModel,
            @DxfgOut final WordPointer symbol
    ) {
        if (symbol.isNull()) {
            throw new IllegalArgumentException("The `symbol` pointer is null");
        }

        final Throwable stored = OlderEntryPoint.takeStoredException();
        final DxfgSymbol result = EventModelNative.dxfg_IndexedEventModel_getSymbol(thread, dxfgIndexedEventModel);

        if (OlderEntryPoint.failed(stored, result.rawValue() == ExceptionHandlerReturnMinusOne.EXECUTE_FAIL)) {
            return ExceptionHandlerReturnMinusOne.EXECUTE_FAIL;
        }

        symbol.write(result);

        return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
    }

    @CEntryPoint(
            name = "dxfg_v4_IndexedEventModel_setSymbol",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_IndexedEventModel_setSymbol(
            final IsolateThread thread,
            final DxfgIndexedEventModel dxfgIndexedEventModel,
            final DxfgSymbol symbol
    ) {
        final Throwable stored = OlderEntryPoint.takeStoredException();
        final int result = EventModelNative.dxfg_IndexedEventModel_setSymbol(thread, dxfgIndexedEventModel, symbol);

        return OlderEntryPoint.failed(stored, result == ExceptionHandlerReturnMinusOne.EXECUTE_FAIL)
                ? ExceptionHandlerReturnMinusOne.EXECUTE_FAIL
                : result;
    }

    @CEntryPoint(
            name = "dxfg_v4_TimeSeriesEventModel_getSymbol",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_TimeSeriesEventModel_getSymbol(
            final IsolateThread thread,
            final DxfgTimeSeriesEventModel dxfgTimeSeriesEventModel,
            @DxfgOut final WordPointer symbol
    ) {
        if (symbol.isNull()) {
            throw new IllegalArgumentException("The `symbol` pointer is null");
        }

        final Throwable stored = OlderEntryPoint.takeStoredException();
        final DxfgSymbol result = EventModelNative.dxfg_TimeSeriesEventModel_getSymbol(thread, dxfgTimeSeriesEventModel);

        if (OlderEntryPoint.failed(stored, result.rawValue() == ExceptionHandlerReturnMinusOne.EXECUTE_FAIL)) {
            return ExceptionHandlerReturnMinusOne.EXECUTE_FAIL;
        }

        symbol.write(result);

        return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
    }

    @CEntryPoint(
            name = "dxfg_v4_TimeSeriesEventModel_setSymbol",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_TimeSeriesEventModel_setSymbol(
            final IsolateThread thread,
            final DxfgTimeSeriesEventModel dxfgTimeSeriesEventModel,
            final DxfgSymbol symbol
    ) {
        final Throwable stored = OlderEntryPoint.takeStoredException();
        final int result = EventModelNative.dxfg_TimeSeriesEventModel_setSymbol(
                        thread,
                        dxfgTimeSeriesEventModel,
                        symbol);

        return OlderEntryPoint.failed(stored, result == ExceptionHandlerReturnMinusOne.EXECUTE_FAIL)
                ? ExceptionHandlerReturnMinusOne.EXECUTE_FAIL
                : result;
    }

    @CEntryPoint(
            name = "dxfg_v4_OrderBookModel_getLotSize",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_OrderBookModel_getLotSize(
            final IsolateThread thread,
            final DxfgOrderBookModel dxfgOrderBookModel,
            @DxfgOut final CInt32Pointer lotSize
    ) {
        if (lotSize.isNull()) {
            throw new IllegalArgumentException("The `lotSize` pointer is null");
        }

        final Throwable stored = OlderEntryPoint.takeStoredException();
        final int result = EventModelNative.dxfg_OrderBookModel_getLotSize(thread, dxfgOrderBookModel);

        if (OlderEntryPoint.failed(stored, result == ExceptionHandlerReturnMinusOne.EXECUTE_FAIL)) {
            return ExceptionHandlerReturnMinusOne.EXECUTE_FAIL;
        }

        lotSize.write(result);

        return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
    }

    @CEntryPoint(
            name = "dxfg_v4_OrderBookModel_setLotSize",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_OrderBookModel_setLotSize(
            final IsolateThread thread,
            final DxfgOrderBookModel dxfgOrderBookModel,
            final int lotSize
    ) {
        final Throwable stored = OlderEntryPoint.takeStoredException();
        final int result = EventModelNative.dxfg_OrderBookModel_setLotSize(thread, dxfgOrderBookModel, lotSize);

        return OlderEntryPoint.failed(stored, result == ExceptionHandlerReturnMinusOne.EXECUTE_FAIL)
                ? ExceptionHandlerReturnMinusOne.EXECUTE_FAIL
                : result;
    }

    @CEntryPoint(
            name = "dxfg_v4_IndexedEventModel_getSizeLimit",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_IndexedEventModel_getSizeLimit(
            final IsolateThread thread,
            final DxfgIndexedEventModel dxfgIndexedEventModel,
            @DxfgOut final CInt32Pointer sizeLimit
    ) {
        if (sizeLimit.isNull()) {
            throw new IllegalArgumentException("The `sizeLimit` pointer is null");
        }

        final Throwable stored = OlderEntryPoint.takeStoredException();
        final int result = EventModelNative.dxfg_IndexedEventModel_getSizeLimit(thread, dxfgIndexedEventModel);

        if (OlderEntryPoint.failed(stored, result == ExceptionHandlerReturnMinusOne.EXECUTE_FAIL)) {
            return ExceptionHandlerReturnMinusOne.EXECUTE_FAIL;
        }

        sizeLimit.write(result);

        return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
    }

    @CEntryPoint(
            name = "dxfg_v4_IndexedEventModel_setSizeLimit",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_IndexedEventModel_setSizeLimit(
            final IsolateThread thread,
            final DxfgIndexedEventModel dxfgIndexedEventModel,
            final int sizeLimit
    ) {
        final Throwable stored = OlderEntryPoint.takeStoredException();
        final int result = EventModelNative.dxfg_IndexedEventModel_setSizeLimit(
                        thread,
                        dxfgIndexedEventModel,
                        sizeLimit);

        return OlderEntryPoint.failed(stored, result == ExceptionHandlerReturnMinusOne.EXECUTE_FAIL)
                ? ExceptionHandlerReturnMinusOne.EXECUTE_FAIL
                : result;
    }

    @CEntryPoint(
            name = "dxfg_v4_TimeSeriesEventModel_getSizeLimit",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_TimeSeriesEventModel_getSizeLimit(
            final IsolateThread thread,
            final DxfgTimeSeriesEventModel dxfgTimeSeriesEventModel,
            @DxfgOut final CInt32Pointer sizeLimit
    ) {
        if (sizeLimit.isNull()) {
            throw new IllegalArgumentException("The `sizeLimit` pointer is null");
        }

        final Throwable stored = OlderEntryPoint.takeStoredException();
        final int result = EventModelNative.dxfg_TimeSeriesEventModel_getSizeLimit(thread, dxfgTimeSeriesEventModel);

        if (OlderEntryPoint.failed(stored, result == ExceptionHandlerReturnMinusOne.EXECUTE_FAIL)) {
            return ExceptionHandlerReturnMinusOne.EXECUTE_FAIL;
        }

        sizeLimit.write(result);

        return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
    }

    @CEntryPoint(
            name = "dxfg_v4_TimeSeriesEventModel_setSizeLimit",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_TimeSeriesEventModel_setSizeLimit(
            final IsolateThread thread,
            final DxfgTimeSeriesEventModel dxfgTimeSeriesEventModel,
            final int sizeLimit
    ) {
        final Throwable stored = OlderEntryPoint.takeStoredException();
        final int result = EventModelNative.dxfg_TimeSeriesEventModel_setSizeLimit(
                        thread,
                        dxfgTimeSeriesEventModel,
                        sizeLimit);

        return OlderEntryPoint.failed(stored, result == ExceptionHandlerReturnMinusOne.EXECUTE_FAIL)
                ? ExceptionHandlerReturnMinusOne.EXECUTE_FAIL
                : result;
    }

    @CEntryPoint(
            name = "dxfg_v4_TimeSeriesEventModel_getFromTime",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_TimeSeriesEventModel_getFromTime(
            final IsolateThread thread,
            final DxfgTimeSeriesEventModel dxfgTimeSeriesEventModel,
            @DxfgOut final CLongPointer fromTime
    ) {
        if (fromTime.isNull()) {
            throw new IllegalArgumentException("The `fromTime` pointer is null");
        }

        final Throwable stored = OlderEntryPoint.takeStoredException();
        final long result = EventModelNative.dxfg_TimeSeriesEventModel_getFromTime(thread, dxfgTimeSeriesEventModel);

        if (OlderEntryPoint.failed(stored, result == ExceptionHandlerReturnMinusOne.EXECUTE_FAIL)) {
            return ExceptionHandlerReturnMinusOne.EXECUTE_FAIL;
        }

        fromTime.write(result);

        return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
    }

    @CEntryPoint(
            name = "dxfg_v4_TimeSeriesEventModel_setFromTime",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_TimeSeriesEventModel_setFromTime(
            final IsolateThread thread,
            final DxfgTimeSeriesEventModel dxfgTimeSeriesEventModel,
            final long fromTime
    ) {
        final Throwable stored = OlderEntryPoint.takeStoredException();
        final int result = EventModelNative.dxfg_TimeSeriesEventModel_setFromTime(
                        thread,
                        dxfgTimeSeriesEventModel,
                        fromTime);

        return OlderEntryPoint.failed(stored, result == ExceptionHandlerReturnMinusOne.EXECUTE_FAIL)
                ? ExceptionHandlerReturnMinusOne.EXECUTE_FAIL
                : result;
    }

    @CEntryPoint(
            name = "dxfg_v4_OrderBookModel_getBuyOrders",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_OrderBookModel_getBuyOrders(
            final IsolateThread thread,
            final DxfgOrderBookModel dxfgOrderBookModel,
            @DxfgOut final WordPointer buyOrders
    ) {
        if (buyOrders.isNull()) {
            throw new IllegalArgumentException("The `buyOrders` pointer is null");
        }

        final Throwable stored = OlderEntryPoint.takeStoredException();
        final DxfgObservableListModel result = EventModelNative.dxfg_OrderBookModel_getBuyOrders(thread, dxfgOrderBookModel);

        if (OlderEntryPoint.failed(stored, result.isNull())) {
            return ExceptionHandlerReturnMinusOne.EXECUTE_FAIL;
        }

        buyOrders.write(result);

        return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
    }

    @CEntryPoint(
            name = "dxfg_v4_OrderBookModel_getSellOrders",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_OrderBookModel_getSellOrders(
            final IsolateThread thread,
            final DxfgOrderBookModel dxfgOrderBookModel,
            @DxfgOut final WordPointer sellOrders
    ) {
        if (sellOrders.isNull()) {
            throw new IllegalArgumentException("The `sellOrders` pointer is null");
        }

        final Throwable stored = OlderEntryPoint.takeStoredException();
        final DxfgObservableListModel result = EventModelNative.dxfg_OrderBookModel_getSellOrders(thread, dxfgOrderBookModel);

        if (OlderEntryPoint.failed(stored, result.isNull())) {
            return ExceptionHandlerReturnMinusOne.EXECUTE_FAIL;
        }

        sellOrders.write(result);

        return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
    }

    @CEntryPoint(
            name = "dxfg_v4_IndexedEventModel_getEventsList",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_IndexedEventModel_getEventsList(
            final IsolateThread thread,
            final DxfgIndexedEventModel dxfgIndexedEventModel,
            @DxfgOut final WordPointer eventsList
    ) {
        if (eventsList.isNull()) {
            throw new IllegalArgumentException("The `eventsList` pointer is null");
        }

        final Throwable stored = OlderEntryPoint.takeStoredException();
        final DxfgObservableListModel result = EventModelNative.dxfg_IndexedEventModel_getEventsList(thread, dxfgIndexedEventModel);

        if (OlderEntryPoint.failed(stored, result.isNull())) {
            return ExceptionHandlerReturnMinusOne.EXECUTE_FAIL;
        }

        eventsList.write(result);

        return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
    }

    @CEntryPoint(
            name = "dxfg_v4_TimeSeriesEventModel_getEventsList",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_TimeSeriesEventModel_getEventsList(
            final IsolateThread thread,
            final DxfgTimeSeriesEventModel dxfgTimeSeriesEventModel,
            @DxfgOut final WordPointer eventsList
    ) {
        if (eventsList.isNull()) {
            throw new IllegalArgumentException("The `eventsList` pointer is null");
        }

        final Throwable stored = OlderEntryPoint.takeStoredException();
        final DxfgObservableListModel result = EventModelNative.dxfg_TimeSeriesEventModel_getEventsList(thread, dxfgTimeSeriesEventModel);

        if (OlderEntryPoint.failed(stored, result.isNull())) {
            return ExceptionHandlerReturnMinusOne.EXECUTE_FAIL;
        }

        eventsList.write(result);

        return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
    }

    @CEntryPoint(
            name = "dxfg_v4_ObservableListModelListener_new",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_ObservableListModelListener_new(
            final IsolateThread thread,
            final DxfgObservableListModelListenerFunction userFunc,
            final VoidPointer userData,
            @DxfgOut final WordPointer observableListModelListener
    ) {
        if (observableListModelListener.isNull()) {
            throw new IllegalArgumentException("The `observableListModelListener` pointer is null");
        }

        final Throwable stored = OlderEntryPoint.takeStoredException();
        final DxfgObservableListModelListener result = EventModelNative.dxfg_ObservableListModelListener_new(thread, userFunc, userData);

        if (OlderEntryPoint.failed(stored, result.isNull())) {
            return ExceptionHandlerReturnMinusOne.EXECUTE_FAIL;
        }

        observableListModelListener.write(result);

        return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
    }

    @CEntryPoint(
            name = "dxfg_v4_ObservableListModel_addListener",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_ObservableListModel_addListener(
            final IsolateThread thread,
            final DxfgObservableListModel dxfgObservableListModel,
            final DxfgObservableListModelListener dxfgObservableListModelListener
    ) {
        final Throwable stored = OlderEntryPoint.takeStoredException();
        final int result = EventModelNative.dxfg_ObservableListModel_addListener(
                        thread,
                        dxfgObservableListModel,
                        dxfgObservableListModelListener);

        return OlderEntryPoint.failed(stored, result == ExceptionHandlerReturnMinusOne.EXECUTE_FAIL)
                ? ExceptionHandlerReturnMinusOne.EXECUTE_FAIL
                : result;
    }

    @CEntryPoint(
            name = "dxfg_v4_ObservableListModel_removeListener",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_ObservableListModel_removeListener(
            final IsolateThread thread,
            final DxfgObservableListModel dxfgObservableListModel,
            final DxfgObservableListModelListener dxfgObservableListModelListener
    ) {
        final Throwable stored = OlderEntryPoint.takeStoredException();
        final int result = EventModelNative.dxfg_ObservableListModel_removeListener(
                        thread,
                        dxfgObservableListModel,
                        dxfgObservableListModelListener);

        return OlderEntryPoint.failed(stored, result == ExceptionHandlerReturnMinusOne.EXECUTE_FAIL)
                ? ExceptionHandlerReturnMinusOne.EXECUTE_FAIL
                : result;
    }

    @CEntryPoint(
            name = "dxfg_v4_OrderBookModelListener_new",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_OrderBookModelListener_new(
            final IsolateThread thread,
            final DxfgOrderBookModelListenerFunction userFunc,
            final VoidPointer userData,
            @DxfgOut final WordPointer orderBookModelListener
    ) {
        if (orderBookModelListener.isNull()) {
            throw new IllegalArgumentException("The `orderBookModelListener` pointer is null");
        }

        final Throwable stored = OlderEntryPoint.takeStoredException();
        final DxfgOrderBookModelListener result = EventModelNative.dxfg_OrderBookModelListener_new(thread, userFunc, userData);

        if (OlderEntryPoint.failed(stored, result.isNull())) {
            return ExceptionHandlerReturnMinusOne.EXECUTE_FAIL;
        }

        orderBookModelListener.write(result);

        return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
    }

    @CEntryPoint(
            name = "dxfg_v4_OrderBookModel_addListener",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_OrderBookModel_addListener(
            final IsolateThread thread,
            final DxfgOrderBookModel dxfgOrderBookModel,
            final DxfgOrderBookModelListener dxfgOrderBookModelListener
    ) {
        final Throwable stored = OlderEntryPoint.takeStoredException();
        final int result = EventModelNative.dxfg_OrderBookModel_addListener(
                        thread,
                        dxfgOrderBookModel,
                        dxfgOrderBookModelListener);

        return OlderEntryPoint.failed(stored, result == ExceptionHandlerReturnMinusOne.EXECUTE_FAIL)
                ? ExceptionHandlerReturnMinusOne.EXECUTE_FAIL
                : result;
    }

    @CEntryPoint(
            name = "dxfg_v4_OrderBookModel_removeListener",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_OrderBookModel_removeListener(
            final IsolateThread thread,
            final DxfgOrderBookModel dxfgOrderBookModel,
            final DxfgOrderBookModelListener dxfgOrderBookModelListener
    ) {
        final Throwable stored = OlderEntryPoint.takeStoredException();
        final int result = EventModelNative.dxfg_OrderBookModel_removeListener(
                        thread,
                        dxfgOrderBookModel,
                        dxfgOrderBookModelListener);

        return OlderEntryPoint.failed(stored, result == ExceptionHandlerReturnMinusOne.EXECUTE_FAIL)
                ? ExceptionHandlerReturnMinusOne.EXECUTE_FAIL
                : result;
    }

    @CEntryPoint(
            name = "dxfg_v4_ObservableListModel_toArray",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_ObservableListModel_toArray(
            final IsolateThread thread,
            final DxfgObservableListModel dxfgObservableListModel,
            @DxfgOut final WordPointer events
    ) {
        if (events.isNull()) {
            throw new IllegalArgumentException("The `events` pointer is null");
        }

        final Throwable stored = OlderEntryPoint.takeStoredException();
        final DxfgEventTypeListPointer result = EventModelNative.dxfg_ObservableListModel_toArray(thread, dxfgObservableListModel);

        if (OlderEntryPoint.failed(stored, result.isNull())) {
            return ExceptionHandlerReturnMinusOne.EXECUTE_FAIL;
        }

        events.write(result);

        return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
    }
}
