// Copyright (c) 2025 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

package com.dxfeed.sdk.orcs;

import com.dxfeed.event.EventType;
import com.dxfeed.event.candle.CandleSymbol;
import com.dxfeed.event.market.Order;
import com.dxfeed.event.market.OrderSource;
import com.dxfeed.orcs.PriceLevelChecker;
import com.dxfeed.sdk.NativeUtils;
import com.dxfeed.sdk.common.CInt32Pointer;
import com.dxfeed.sdk.common.DxfgOut;
import com.dxfeed.sdk.events.DxfgEventTypeListPointer;
import com.dxfeed.sdk.events.DxfgEventTypeListPointerPointer;
import com.dxfeed.sdk.exception.ExceptionHandlerReturnMinusOne;
import com.dxfeed.sdk.orcs.mappers.Mappers;
import com.dxfeed.sdk.source.DxfgIndexedEventSource;
import com.dxfeed.sdk.symbol.DxfgSymbol;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
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
                Mappers.PRICE_LEVEL_SERVICE_MAPPER.toNative(
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
                NativeUtils.MAPPER_EVENTS.toNativeList(Mappers.PRICE_LEVEL_SERVICE_MAPPER.toJava(service)
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
                NativeUtils.MAPPER_EVENTS.toNativeList(Mappers.PRICE_LEVEL_SERVICE_MAPPER.toJava(service)
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
                Mappers.AUTH_ORDER_SOURCE_MAPPER.toNative(Mappers.PRICE_LEVEL_SERVICE_MAPPER.toJava(service)
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
                NativeUtils.MAPPER_EVENTS.toNativeList(Mappers.PRICE_LEVEL_SERVICE_MAPPER.toJava(service)
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
                NativeUtils.MAPPER_EVENTS.toNativeList(Mappers.PRICE_LEVEL_SERVICE_MAPPER.toJava(service)
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
        Mappers.PRICE_LEVEL_SERVICE_MAPPER.toJava(service).close();

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
                Mappers.SYMBOLS_BY_ORDER_SOURCE_ID_MAP_ENTRY_LIST_MAPPER.toNativeList(
                        Mappers.AUTH_ORDER_SOURCE_MAPPER.toJava(authOrderSource)
                                .getByIds().entrySet()));

        return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
    }

    @CEntryPoint(name = "dxfg_AuthOrderSource_getByOrderSources", exceptionHandler = ExceptionHandlerReturnMinusOne.class)
    public static int dxfg_AuthOrderSource_getByOrderSources(final IsolateThread ignoredThread,
            final DxfgAuthOrderSourceHandle authOrderSource,
            @DxfgOut final DxfgSymbolsByOrderSourceMapEntryListPointerPointer symbolsByOrderSourceMapEntryList

    ) {
        if (symbolsByOrderSourceMapEntryList.isNull()) {
            throw new IllegalArgumentException("The `symbolsByOrderSourceMapEntryList` pointer is null");
        }

        //noinspection DataFlowIssue
        symbolsByOrderSourceMapEntryList.write(
                Mappers.SYMBOLS_BY_ORDER_SOURCE_MAP_ENTRY_LIST_MAPPER.toNativeList(
                        Mappers.AUTH_ORDER_SOURCE_MAPPER.toJava(authOrderSource)
                                .getByOrderSources().entrySet()));

        return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
    }

    @CEntryPoint(name = "dxfg_PriceLevelChecker_validate", exceptionHandler = ExceptionHandlerReturnMinusOne.class)
    public static int dxfg_PriceLevelChecker_validate(final IsolateThread ignoredThread,
            final DxfgEventTypeListPointer orders,
            long timeGapBound, int printQuotes,
            @DxfgOut final CInt32Pointer isValid) {
        if (isValid.isNull()) {
            throw new IllegalArgumentException("The `isValid` pointer is null");
        }

        List<Order> list = NativeUtils.MAPPER_EVENTS.toJavaList(orders).stream().map(eventType -> (Order) eventType)
                .collect(Collectors.toList());

        isValid.write(
                PriceLevelChecker.validate(list,
                        timeGapBound,
                        printQuotes != 0)
                        ? 1 : 0);

        return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
    }
}
