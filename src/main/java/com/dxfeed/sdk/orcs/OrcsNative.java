// Copyright (c) 2025 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

package com.dxfeed.sdk.orcs;

import com.dxfeed.event.candle.CandleSymbol;
import com.dxfeed.event.market.OrderSource;
import com.dxfeed.sdk.NativeUtils;
import com.dxfeed.sdk.common.DxfgOut;
import com.dxfeed.sdk.events.DxfgEventTypeListPointerPointer;
import com.dxfeed.sdk.exception.ExceptionHandlerReturnMinusOne;
import com.dxfeed.sdk.source.DxfgIndexedEventSource;
import com.dxfeed.sdk.symbol.DxfgSymbol;
import java.io.IOException;
import org.graalvm.nativeimage.IsolateThread;
import org.graalvm.nativeimage.c.CContext;
import org.graalvm.nativeimage.c.function.CEntryPoint;
import org.graalvm.nativeimage.c.type.CCharPointer;
import org.graalvm.nativeimage.c.type.CConst;

@CContext(Directives.class)
public class OrcsNative {

    @CEntryPoint(name = "dxfg_PriceLevelService_new", exceptionHandler = ExceptionHandlerReturnMinusOne.class)
    public static int dxfg_PriceLevelService_new(final IsolateThread ignoredThread,
            @CConst final CCharPointer address,
            @DxfgOut final DxfgPriceLevelServiceHandlePointer service) {
        if (service.isNull()) {
            throw new IllegalArgumentException("The `service` pointer is null");
        }

        service.write(
                NativeUtils.MAPPER_PRICE_LEVEL_SERVICE.toNative(
                        new PriceLevelServiceHolder(NativeUtils.MAPPER_STRING.toJava(address))));

        return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
    }

    @CEntryPoint(name = "dxfg_PriceLevelService_getOrders", exceptionHandler = ExceptionHandlerReturnMinusOne.class)
    public static int dxfg_PriceLevelService_getOrders(final IsolateThread ignoredThread,
            final DxfgPriceLevelServiceHandle service,
            final DxfgSymbol candleSymbol,
            final DxfgIndexedEventSource orderSource,
            final long from, final long to,
            @CConst final CCharPointer caller,
            @DxfgOut final DxfgEventTypeListPointerPointer orders

    ) {
        if (orders.isNull()) {
            throw new IllegalArgumentException("The `orders` pointer is null");
        }

        //noinspection DataFlowIssue
        orders.write(
                NativeUtils.MAPPER_EVENTS.toNativeList(NativeUtils.MAPPER_PRICE_LEVEL_SERVICE.toJava(service)
                        .getOrders((CandleSymbol) NativeUtils.MAPPER_SYMBOL.toJava(candleSymbol),
                                (OrderSource) NativeUtils.MAPPER_INDEXED_EVENT_SOURCE.toJava(orderSource), from, to,
                                NativeUtils.MAPPER_STRING.toJava(caller)
                        )));

        return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
    }

    @CEntryPoint(name = "dxfg_PriceLevelService_getOrders2", exceptionHandler = ExceptionHandlerReturnMinusOne.class)
    public static int dxfg_PriceLevelService_getOrders2(final IsolateThread ignoredThread,
            final DxfgPriceLevelServiceHandle service,
            final DxfgSymbol candleSymbol,
            final DxfgIndexedEventSource orderSource,
            final long from, final long to,
            @DxfgOut final DxfgEventTypeListPointerPointer orders

    ) {
        if (orders.isNull()) {
            throw new IllegalArgumentException("The `orders` pointer is null");
        }

        //noinspection DataFlowIssue
        orders.write(
                NativeUtils.MAPPER_EVENTS.toNativeList(NativeUtils.MAPPER_PRICE_LEVEL_SERVICE.toJava(service)
                        .getOrders((CandleSymbol) NativeUtils.MAPPER_SYMBOL.toJava(candleSymbol),
                                (OrderSource) NativeUtils.MAPPER_INDEXED_EVENT_SOURCE.toJava(orderSource), from, to
                        )));

        return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
    }

    @CEntryPoint(name = "dxfg_PriceLevelService_getAuthOrderSource", exceptionHandler = ExceptionHandlerReturnMinusOne.class)
    public static int dxfg_PriceLevelService_getAuthOrderSource(final IsolateThread ignoredThread,
            final DxfgPriceLevelServiceHandle service,
            @DxfgOut final DxfgAuthOrderSourceHandlePointer authOrderSource

    ) {
        if (authOrderSource.isNull()) {
            throw new IllegalArgumentException("The `authOrderSource` pointer is null");
        }

        //noinspection DataFlowIssue
        authOrderSource.write(
                NativeUtils.MAPPER_AUTH_ORDER_SOURCE.toNative(NativeUtils.MAPPER_PRICE_LEVEL_SERVICE.toJava(service)
                        .getAuthOrderSource()));

        return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
    }

    @CEntryPoint(name = "dxfg_PriceLevelService_getQuotes", exceptionHandler = ExceptionHandlerReturnMinusOne.class)
    public static int dxfg_PriceLevelService_getQuotes(final IsolateThread ignoredThread,
            final DxfgPriceLevelServiceHandle service,
            final DxfgSymbol candleSymbol,
            final long from, final long to,
            @CConst final CCharPointer caller,
            @DxfgOut final DxfgEventTypeListPointerPointer quotes

    ) {
        if (quotes.isNull()) {
            throw new IllegalArgumentException("The `quotes` pointer is null");
        }

        //noinspection DataFlowIssue
        quotes.write(
                NativeUtils.MAPPER_EVENTS.toNativeList(NativeUtils.MAPPER_PRICE_LEVEL_SERVICE.toJava(service)
                        .getQuotes((CandleSymbol) NativeUtils.MAPPER_SYMBOL.toJava(candleSymbol),
                                from, to,
                                NativeUtils.MAPPER_STRING.toJava(caller)
                        )));

        return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
    }

    @CEntryPoint(name = "dxfg_PriceLevelService_getQuotes2", exceptionHandler = ExceptionHandlerReturnMinusOne.class)
    public static int dxfg_PriceLevelService_getQuotes2(final IsolateThread ignoredThread,
            final DxfgPriceLevelServiceHandle service,
            final DxfgSymbol candleSymbol,
            final long from, final long to,
            @DxfgOut final DxfgEventTypeListPointerPointer quotes

    ) {
        if (quotes.isNull()) {
            throw new IllegalArgumentException("The `quotes` pointer is null");
        }

        //noinspection DataFlowIssue
        quotes.write(
                NativeUtils.MAPPER_EVENTS.toNativeList(NativeUtils.MAPPER_PRICE_LEVEL_SERVICE.toJava(service)
                        .getQuotes((CandleSymbol) NativeUtils.MAPPER_SYMBOL.toJava(candleSymbol),
                                from, to
                        )));

        return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
    }

    @CEntryPoint(name = "dxfg_PriceLevelService_close", exceptionHandler = ExceptionHandlerReturnMinusOne.class)
    public static int dxfg_PriceLevelService_close(final IsolateThread ignoredThread,
            final DxfgPriceLevelServiceHandle service

    ) throws IOException {
        //noinspection DataFlowIssue
        NativeUtils.MAPPER_PRICE_LEVEL_SERVICE.toJava(service).close();

        return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
    }

    @CEntryPoint(name = "dxfg_AuthOrderSource_getByIds", exceptionHandler = ExceptionHandlerReturnMinusOne.class)
    public static int dxfg_AuthOrderSource_getByIds(final IsolateThread ignoredThread,
            final DxfgAuthOrderSourceHandle authOrderSource,
            @DxfgOut final DxfgSymbolsByOrderSourceIdMapEntryListPointerPointer symbolsByOrderSourceIdMapEntryList

    ) {
        if (symbolsByOrderSourceIdMapEntryList.isNull()) {
            throw new IllegalArgumentException("The `symbolsByOrderSourceIdMapEntryList` pointer is null");
        }

        //noinspection DataFlowIssue
        symbolsByOrderSourceIdMapEntryList.write(
                NativeUtils.MAPPER_EVENTS.toNativeList(NativeUtils.MAPPER_PRICE_LEVEL_SERVICE.toJava(service)
                        .getQuotes((CandleSymbol) NativeUtils.MAPPER_SYMBOL.toJava(candleSymbol),
                                from, to
                        )));

        return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
    }
}
