// Copyright (c) 2026 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

package com.dxfeed.sdk.orcs;

import com.dxfeed.sdk.common.CInt32Pointer;
import com.dxfeed.sdk.common.DxfgOut;
import com.dxfeed.sdk.events.DxfgEventTypeListPointer;
import com.dxfeed.sdk.events.DxfgEventTypeListPointerPointer;
import com.dxfeed.sdk.exception.ExceptionHandlerReturnMinusOne;
import com.dxfeed.sdk.exception.OlderEntryPoint;
import com.dxfeed.sdk.source.DxfgIndexedEventSourcePointer;
import com.dxfeed.sdk.symbol.DxfgSymbol;
import java.io.IOException;
import org.graalvm.nativeimage.IsolateThread;
import org.graalvm.nativeimage.c.CContext;
import org.graalvm.nativeimage.c.function.CEntryPoint;
import org.graalvm.nativeimage.c.type.CCharPointer;
import org.graalvm.nativeimage.c.type.CConst;

/**
 * The v4 twins of the functions of {@link OrcsNative}: each one calls the old function as a Java method
 * ({@link OlderEntryPoint} tells whether it failed) and returns its result through an out parameter (see
 * dxfg_v4_api.h).
 */
@CContext(OrcsDirectives.class)
public final class OrcsV4Native {

    private OrcsV4Native() {
    }

    @CEntryPoint(
            name = "dxfg_v4_PriceLevelService_new",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_PriceLevelService_new(
            final IsolateThread thread,
            @CConst final CCharPointer address,
            @DxfgOut final DxfgPriceLevelServiceHandlePointer service
    ) {
        final Throwable stored = OlderEntryPoint.takeStoredException();
        final int result = OrcsNative.dxfg_PriceLevelService_new(thread, address, service);

        return OlderEntryPoint.failed(stored, result == ExceptionHandlerReturnMinusOne.EXECUTE_FAIL)
                ? ExceptionHandlerReturnMinusOne.EXECUTE_FAIL
                : result;
    }

    @CEntryPoint(
            name = "dxfg_v4_PriceLevelService_getOrdersWithCaller",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_PriceLevelService_getOrdersWithCaller(
            final IsolateThread thread,
            final DxfgPriceLevelServiceHandle service,
            final DxfgSymbol candleSymbol,
            final DxfgIndexedEventSourcePointer orderSource,
            final long from,
            final long to,
            @CConst final CCharPointer caller,
            @DxfgOut final DxfgEventTypeListPointerPointer orders
    ) {
        final Throwable stored = OlderEntryPoint.takeStoredException();
        final int result = OrcsNative.dxfg_PriceLevelService_getOrders(
                        thread,
                        service,
                        candleSymbol,
                        orderSource,
                        from,
                        to,
                        caller,
                        orders);

        return OlderEntryPoint.failed(stored, result == ExceptionHandlerReturnMinusOne.EXECUTE_FAIL)
                ? ExceptionHandlerReturnMinusOne.EXECUTE_FAIL
                : result;
    }

    @CEntryPoint(
            name = "dxfg_v4_PriceLevelService_getOrders",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_PriceLevelService_getOrders(
            final IsolateThread thread,
            final DxfgPriceLevelServiceHandle service,
            final DxfgSymbol candleSymbol,
            final DxfgIndexedEventSourcePointer orderSource,
            final long from,
            final long to,
            @DxfgOut final DxfgEventTypeListPointerPointer orders
    ) {
        final Throwable stored = OlderEntryPoint.takeStoredException();
        final int result = OrcsNative.dxfg_PriceLevelService_getOrders2(
                        thread,
                        service,
                        candleSymbol,
                        orderSource,
                        from,
                        to,
                        orders);

        return OlderEntryPoint.failed(stored, result == ExceptionHandlerReturnMinusOne.EXECUTE_FAIL)
                ? ExceptionHandlerReturnMinusOne.EXECUTE_FAIL
                : result;
    }

    @CEntryPoint(
            name = "dxfg_v4_PriceLevelService_getAuthOrderSource",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_PriceLevelService_getAuthOrderSource(
            final IsolateThread thread,
            final DxfgPriceLevelServiceHandle service,
            @DxfgOut final DxfgAuthOrderSourceHandlePointer authOrderSource
    ) {
        final Throwable stored = OlderEntryPoint.takeStoredException();
        final int result = OrcsNative.dxfg_PriceLevelService_getAuthOrderSource(thread, service, authOrderSource);

        return OlderEntryPoint.failed(stored, result == ExceptionHandlerReturnMinusOne.EXECUTE_FAIL)
                ? ExceptionHandlerReturnMinusOne.EXECUTE_FAIL
                : result;
    }

    @CEntryPoint(
            name = "dxfg_v4_PriceLevelService_getQuotesWithCaller",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_PriceLevelService_getQuotesWithCaller(
            final IsolateThread thread,
            final DxfgPriceLevelServiceHandle service,
            final DxfgSymbol candleSymbol,
            final long from,
            final long to,
            @CConst final CCharPointer caller,
            @DxfgOut final DxfgEventTypeListPointerPointer quotes
    ) {
        final Throwable stored = OlderEntryPoint.takeStoredException();
        final int result = OrcsNative.dxfg_PriceLevelService_getQuotes(
                        thread,
                        service,
                        candleSymbol,
                        from,
                        to,
                        caller,
                        quotes);

        return OlderEntryPoint.failed(stored, result == ExceptionHandlerReturnMinusOne.EXECUTE_FAIL)
                ? ExceptionHandlerReturnMinusOne.EXECUTE_FAIL
                : result;
    }

    @CEntryPoint(
            name = "dxfg_v4_PriceLevelService_getQuotes",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_PriceLevelService_getQuotes(
            final IsolateThread thread,
            final DxfgPriceLevelServiceHandle service,
            final DxfgSymbol candleSymbol,
            final long from,
            final long to,
            @DxfgOut final DxfgEventTypeListPointerPointer quotes
    ) {
        final Throwable stored = OlderEntryPoint.takeStoredException();
        final int result = OrcsNative.dxfg_PriceLevelService_getQuotes2(
                        thread,
                        service,
                        candleSymbol,
                        from,
                        to,
                        quotes);

        return OlderEntryPoint.failed(stored, result == ExceptionHandlerReturnMinusOne.EXECUTE_FAIL)
                ? ExceptionHandlerReturnMinusOne.EXECUTE_FAIL
                : result;
    }

    @CEntryPoint(
            name = "dxfg_v4_PriceLevelService_close",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_PriceLevelService_close(
            final IsolateThread thread,
            final DxfgPriceLevelServiceHandle service
    ) throws IOException {
        final Throwable stored = OlderEntryPoint.takeStoredException();
        final int result = OrcsNative.dxfg_PriceLevelService_close(thread, service);

        return OlderEntryPoint.failed(stored, result == ExceptionHandlerReturnMinusOne.EXECUTE_FAIL)
                ? ExceptionHandlerReturnMinusOne.EXECUTE_FAIL
                : result;
    }

    @CEntryPoint(
            name = "dxfg_v4_AuthOrderSource_getByIds",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_AuthOrderSource_getByIds(
            final IsolateThread thread,
            final DxfgAuthOrderSourceHandle authOrderSource,
            @DxfgOut final DxfgSymbolsByOrderSourceIdMapEntryListPointerPointer symbolsByOrderSourceIdMapEntryList
    ) {
        final Throwable stored = OlderEntryPoint.takeStoredException();
        final int result = OrcsNative.dxfg_AuthOrderSource_getByIds(
                        thread,
                        authOrderSource,
                        symbolsByOrderSourceIdMapEntryList);

        return OlderEntryPoint.failed(stored, result == ExceptionHandlerReturnMinusOne.EXECUTE_FAIL)
                ? ExceptionHandlerReturnMinusOne.EXECUTE_FAIL
                : result;
    }

    @CEntryPoint(
            name = "dxfg_v4_symbols_by_order_source_id_map_entry_list_release",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_symbols_by_order_source_id_map_entry_list_release(
            final IsolateThread thread,
            @CConst final DxfgSymbolsByOrderSourceIdMapEntryListPointer list
    ) {
        final Throwable stored = OlderEntryPoint.takeStoredException();
        final int result = OrcsNative.dxfgSymbolsByOrderSourceIdMapEntryListFree(thread, list);

        return OlderEntryPoint.failed(stored, result == ExceptionHandlerReturnMinusOne.EXECUTE_FAIL)
                ? ExceptionHandlerReturnMinusOne.EXECUTE_FAIL
                : result;
    }

    @CEntryPoint(
            name = "dxfg_v4_AuthOrderSource_getByOrderSources",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_AuthOrderSource_getByOrderSources(
            final IsolateThread thread,
            final DxfgAuthOrderSourceHandle authOrderSource,
            @DxfgOut final DxfgSymbolsByOrderSourceMapEntryListPointerPointer symbolsByOrderSourceMapEntryList
    ) {
        final Throwable stored = OlderEntryPoint.takeStoredException();
        final int result = OrcsNative.dxfg_AuthOrderSource_getByOrderSources(
                        thread,
                        authOrderSource,
                        symbolsByOrderSourceMapEntryList);

        return OlderEntryPoint.failed(stored, result == ExceptionHandlerReturnMinusOne.EXECUTE_FAIL)
                ? ExceptionHandlerReturnMinusOne.EXECUTE_FAIL
                : result;
    }

    @CEntryPoint(
            name = "dxfg_v4_symbols_by_order_source_map_entry_list_release",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_symbols_by_order_source_map_entry_list_release(
            final IsolateThread thread,
            @CConst final DxfgSymbolsByOrderSourceMapEntryListPointer list
    ) {
        final Throwable stored = OlderEntryPoint.takeStoredException();
        final int result = OrcsNative.dxfgSymbolsByOrderSourceMapEntryListFree(thread, list);

        return OlderEntryPoint.failed(stored, result == ExceptionHandlerReturnMinusOne.EXECUTE_FAIL)
                ? ExceptionHandlerReturnMinusOne.EXECUTE_FAIL
                : result;
    }

    @CEntryPoint(
            name = "dxfg_v4_PriceLevelChecker_validate",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_PriceLevelChecker_validate(
            final IsolateThread thread,
            final DxfgEventTypeListPointer orders,
            final long timeGapBound,
            final int printQuotes,
            @DxfgOut final CInt32Pointer isValid
    ) {
        final Throwable stored = OlderEntryPoint.takeStoredException();
        final int result = OrcsNative.dxfg_PriceLevelChecker_validate(
                        thread,
                        orders,
                        timeGapBound,
                        printQuotes,
                        isValid);

        return OlderEntryPoint.failed(stored, result == ExceptionHandlerReturnMinusOne.EXECUTE_FAIL)
                ? ExceptionHandlerReturnMinusOne.EXECUTE_FAIL
                : result;
    }
}
