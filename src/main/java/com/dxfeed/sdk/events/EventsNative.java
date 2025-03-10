// Copyright (c) 2025 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

package com.dxfeed.sdk.events;

import com.dxfeed.api.osub.WildcardSymbol;
import com.dxfeed.event.IndexedEventSource;
import com.dxfeed.event.candle.CandleSymbol;
import com.dxfeed.event.market.EventMappers;
import com.dxfeed.event.market.OrderSource;
import com.dxfeed.sdk.NativeUtils;
import com.dxfeed.sdk.exception.ExceptionHandlerReturnMinusOne;
import com.dxfeed.sdk.exception.ExceptionHandlerReturnNullWord;
import com.dxfeed.sdk.source.DxfgIndexedEventSource;
import com.dxfeed.sdk.symbol.DxfgSymbol;
import com.dxfeed.sdk.symbol.DxfgSymbolList;
import com.dxfeed.sdk.symbol.DxfgSymbolType;
import org.graalvm.nativeimage.IsolateThread;
import org.graalvm.nativeimage.c.CContext;
import org.graalvm.nativeimage.c.function.CEntryPoint;
import org.graalvm.nativeimage.c.type.CCharPointer;

@CContext(Directives.class)
public class EventsNative {

    @CEntryPoint(
            name = "dxfg_Symbol_new",
            exceptionHandler = ExceptionHandlerReturnNullWord.class
    )
    public static DxfgSymbol dxfg_Symbol_new(
            final IsolateThread ignoredThread,
            final CCharPointer symbol,
            final DxfgSymbolType symbolType
    ) {
        return switch (symbolType) {
            case STRING -> NativeUtils.MAPPER_SYMBOL.toNative(
                    NativeUtils.MAPPER_STRING.toJava(symbol)
            );
            case CANDLE -> NativeUtils.MAPPER_SYMBOL.toNative(
                    CandleSymbol.valueOf(NativeUtils.MAPPER_STRING.toJava(symbol))
            );
            case WILDCARD -> NativeUtils.MAPPER_SYMBOL.toNative(WildcardSymbol.ALL);
            default -> throw new IllegalStateException();
        };
    }

    @CEntryPoint(
            name = "dxfg_Symbol_release",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_Symbol_release(
            final IsolateThread ignoredThread,
            final DxfgSymbol symbol
    ) {
        NativeUtils.MAPPER_SYMBOL.release(symbol);
        return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
    }

    @CEntryPoint(
            name = "dxfg_EventType_new",
            exceptionHandler = ExceptionHandlerReturnNullWord.class
    )
    public static DxfgEventType dxfg_EventType_new(
            final IsolateThread ignoredThread,
            final CCharPointer symbol,
            final DxfgEventClazz dxfgEventClazz
    ) {
        return ((EventMappers) NativeUtils.MAPPER_EVENT).createNativeEvent(dxfgEventClazz, symbol);
    }

    @CEntryPoint(
            name = "dxfg_EventType_release",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int releaseEvent(
            final IsolateThread ignoredThread,
            final DxfgEventType dxfgEventType
    ) {
        NativeUtils.MAPPER_EVENT.release(dxfgEventType);
        return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
    }

    @CEntryPoint(
            name = "dxfg_CList_EventType_release",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_CList_EventType_release(
            final IsolateThread ignoredThread,
            final DxfgEventTypeList nEvents
    ) {
        NativeUtils.MAPPER_EVENTS.release(nEvents);
        return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
    }

    @CEntryPoint(
            name = "dxfg_CList_EventClazz_release",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_CList_EventClazz_release(
            final IsolateThread ignoredThread,
            final DxfgEventClazzList eventClazzList
    ) {
        NativeUtils.MAPPER_EVENT_TYPES.release(eventClazzList);
        return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
    }

    @CEntryPoint(
            name = "dxfg_CList_symbol_release",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_CList_symbol_release(
            final IsolateThread ignoredThread,
            final DxfgSymbolList symbolList
    ) {
        NativeUtils.MAPPER_SYMBOLS.release(symbolList);
        return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
    }

    @CEntryPoint(
            name = "dxfg_IndexedEventSource_new",
            exceptionHandler = ExceptionHandlerReturnNullWord.class
    )
    public static DxfgIndexedEventSource dxfg_IndexedEventSource_new(
            final IsolateThread ignoredThread,
            final CCharPointer source
    ) {
        return NativeUtils.MAPPER_INDEXED_EVENT_SOURCE.toNative(
                source.isNull()
                        ? IndexedEventSource.DEFAULT
                        : OrderSource.valueOf(NativeUtils.MAPPER_STRING.toJava(source))
        );
    }

    @CEntryPoint(
            name = "dxfg_IndexedEventSource_release",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_IndexedEventSource_release(
            final IsolateThread ignoredThread,
            final DxfgIndexedEventSource source
    ) {
        NativeUtils.MAPPER_INDEXED_EVENT_SOURCE.release(source);
        return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
    }

    @CEntryPoint(
            name = "dxfg_IndexedEvent_getSource",
            exceptionHandler = ExceptionHandlerReturnNullWord.class
    )
    public static DxfgIndexedEventSource dxfg_IndexedEvent_getSource(
            final IsolateThread ignoredThread,
            final DxfgEventType event
    ) {
        switch (DxfgEventClazz.fromCValue(event.getClazz())) {
            case DXFG_EVENT_GREEKS:
            case DXFG_EVENT_CANDLE:
            case DXFG_EVENT_DAILY_CANDLE:
            case DXFG_EVENT_TIME_AND_SALE:
            case DXFG_EVENT_UNDERLYING:
            case DXFG_EVENT_THEO_PRICE:
            case DXFG_EVENT_SERIES:
                return NativeUtils.MAPPER_INDEXED_EVENT_SOURCE.toNative(IndexedEventSource.DEFAULT);
            case DXFG_EVENT_ORDER_BASE:
            case DXFG_EVENT_SPREAD_ORDER:
            case DXFG_EVENT_ORDER:
            case DXFG_EVENT_ANALYTIC_ORDER:
                final long index = ((DxfgOrderBase) event).getIndex();
                int sourceId = (int) (index >> 48);
                if (!OrderSource.isSpecialSourceId(sourceId)) {
                    sourceId = (int) (index >> 32);
                }
                return NativeUtils.MAPPER_INDEXED_EVENT_SOURCE.toNative(OrderSource.valueOf(sourceId));
            default:
                throw new ClassCastException(
                        DxfgEventClazz.fromCValue(event.getClazz()).clazz.getCanonicalName()
                                + " is not Class<? extends IndexedEvent>"
                );
        }
    }
}
