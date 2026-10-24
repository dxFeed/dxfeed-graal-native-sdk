// Copyright (c) 2026 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

package com.dxfeed.sdk.events;

import com.dxfeed.sdk.common.DxfgOut;
import com.dxfeed.sdk.exception.ExceptionHandlerReturnMinusOne;
import com.dxfeed.sdk.exception.OlderEntryPoint;
import com.dxfeed.sdk.source.DxfgIndexedEventSourcePointer;
import com.dxfeed.sdk.source.DxfgIndexedEventSourcePointerPointer;
import com.dxfeed.sdk.symbol.DxfgSymbol;
import com.dxfeed.sdk.symbol.DxfgSymbolList;
import com.dxfeed.sdk.symbol.DxfgSymbolType;
import org.graalvm.nativeimage.IsolateThread;
import org.graalvm.nativeimage.c.CContext;
import org.graalvm.nativeimage.c.function.CEntryPoint;
import org.graalvm.nativeimage.c.type.CCharPointer;
import org.graalvm.nativeimage.c.type.WordPointer;

/**
 * The v4 twins of the functions of {@link EventsNative}: each one calls the old function as a Java method
 * ({@link OlderEntryPoint} tells whether it failed) and returns its result through an out parameter (see
 * dxfg_v4_api.h).
 */
@CContext(EventsDirectives.class)
public final class EventsV4Native {

    private EventsV4Native() {
    }

    @CEntryPoint(
            name = "dxfg_v4_Symbol_new",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_Symbol_new(
            final IsolateThread thread,
            final CCharPointer symbol,
            final DxfgSymbolType symbolType,
            @DxfgOut final WordPointer newSymbol
    ) {
        if (newSymbol.isNull()) {
            throw new IllegalArgumentException("The `newSymbol` pointer is null");
        }

        final Throwable stored = OlderEntryPoint.takeStoredException();
        final DxfgSymbol result = EventsNative.dxfg_Symbol_new(thread, symbol, symbolType);

        if (OlderEntryPoint.failed(stored, result.isNull())) {
            return ExceptionHandlerReturnMinusOne.EXECUTE_FAIL;
        }

        newSymbol.write(result);

        return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
    }

    @CEntryPoint(
            name = "dxfg_v4_Symbol_release",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_Symbol_release(
            final IsolateThread thread,
            final DxfgSymbol symbol
    ) {
        final Throwable stored = OlderEntryPoint.takeStoredException();
        final int result = EventsNative.dxfg_Symbol_release(thread, symbol);

        return OlderEntryPoint.failed(stored, result == ExceptionHandlerReturnMinusOne.EXECUTE_FAIL)
                ? ExceptionHandlerReturnMinusOne.EXECUTE_FAIL
                : result;
    }

    @CEntryPoint(
            name = "dxfg_v4_EventType_new",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_EventType_new(
            final IsolateThread thread,
            final CCharPointer symbol,
            final DxfgEventClazz dxfgEventClazz,
            @DxfgOut final WordPointer event
    ) {
        if (event.isNull()) {
            throw new IllegalArgumentException("The `event` pointer is null");
        }

        final Throwable stored = OlderEntryPoint.takeStoredException();
        final DxfgEventType result = EventsNative.dxfg_EventType_new(thread, symbol, dxfgEventClazz);

        if (OlderEntryPoint.failed(stored, result.isNull())) {
            return ExceptionHandlerReturnMinusOne.EXECUTE_FAIL;
        }

        event.write(result);

        return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
    }

    @CEntryPoint(
            name = "dxfg_v4_EventType_release",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_EventType_release(
            final IsolateThread thread,
            final DxfgEventType dxfgEventType
    ) {
        final Throwable stored = OlderEntryPoint.takeStoredException();
        final int result = EventsNative.releaseEvent(thread, dxfgEventType);

        return OlderEntryPoint.failed(stored, result == ExceptionHandlerReturnMinusOne.EXECUTE_FAIL)
                ? ExceptionHandlerReturnMinusOne.EXECUTE_FAIL
                : result;
    }

    @CEntryPoint(
            name = "dxfg_v4_event_type_list_release",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_event_type_list_release(
            final IsolateThread thread,
            final DxfgEventTypeListPointer events
    ) {
        final Throwable stored = OlderEntryPoint.takeStoredException();
        final int result = EventsNative.dxfg_CList_EventType_release(thread, events);

        return OlderEntryPoint.failed(stored, result == ExceptionHandlerReturnMinusOne.EXECUTE_FAIL)
                ? ExceptionHandlerReturnMinusOne.EXECUTE_FAIL
                : result;
    }

    @CEntryPoint(
            name = "dxfg_v4_event_clazz_list_release",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_event_clazz_list_release(
            final IsolateThread thread,
            final DxfgEventClazzList eventClazzList
    ) {
        final Throwable stored = OlderEntryPoint.takeStoredException();
        final int result = EventsNative.dxfg_CList_EventClazz_release(thread, eventClazzList);

        return OlderEntryPoint.failed(stored, result == ExceptionHandlerReturnMinusOne.EXECUTE_FAIL)
                ? ExceptionHandlerReturnMinusOne.EXECUTE_FAIL
                : result;
    }

    @CEntryPoint(
            name = "dxfg_v4_symbol_list_release",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_symbol_list_release(
            final IsolateThread thread,
            final DxfgSymbolList symbolList
    ) {
        final Throwable stored = OlderEntryPoint.takeStoredException();
        final int result = EventsNative.dxfg_CList_symbol_release(thread, symbolList);

        return OlderEntryPoint.failed(stored, result == ExceptionHandlerReturnMinusOne.EXECUTE_FAIL)
                ? ExceptionHandlerReturnMinusOne.EXECUTE_FAIL
                : result;
    }

    @CEntryPoint(
            name = "dxfg_v4_IndexedEvent_getSource",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_IndexedEvent_getSource(
            final IsolateThread thread,
            final DxfgEventType event,
            @DxfgOut final WordPointer source
    ) {
        if (source.isNull()) {
            throw new IllegalArgumentException("The `source` pointer is null");
        }

        final Throwable stored = OlderEntryPoint.takeStoredException();
        final DxfgIndexedEventSourcePointer result = EventsNative.dxfg_IndexedEvent_getSource(thread, event);

        if (OlderEntryPoint.failed(stored, result.isNull())) {
            return ExceptionHandlerReturnMinusOne.EXECUTE_FAIL;
        }

        source.write(result);

        return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
    }

    @CEntryPoint(
            name = "dxfg_v4_IndexedEventSource_new",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_IndexedEventSource_new(
            final IsolateThread thread,
            final CCharPointer source,
            @DxfgOut final WordPointer newSource
    ) {
        if (newSource.isNull()) {
            throw new IllegalArgumentException("The `newSource` pointer is null");
        }

        final Throwable stored = OlderEntryPoint.takeStoredException();
        final DxfgIndexedEventSourcePointer result = EventsNative.dxfg_IndexedEventSource_new(thread, source);

        if (OlderEntryPoint.failed(stored, result.isNull())) {
            return ExceptionHandlerReturnMinusOne.EXECUTE_FAIL;
        }

        newSource.write(result);

        return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
    }

    @CEntryPoint(
            name = "dxfg_v4_IndexedEventSource_newWithId",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_IndexedEventSource_newWithId(
            final IsolateThread thread,
            final int sourceId,
            @DxfgOut final DxfgIndexedEventSourcePointerPointer source
    ) {
        final Throwable stored = OlderEntryPoint.takeStoredException();
        final int result = EventsNative.dxfg_IndexedEventSource_new2(thread, sourceId, source);

        return OlderEntryPoint.failed(stored, result == ExceptionHandlerReturnMinusOne.EXECUTE_FAIL)
                ? ExceptionHandlerReturnMinusOne.EXECUTE_FAIL
                : result;
    }

    @CEntryPoint(
            name = "dxfg_v4_IndexedEventSource_release",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_IndexedEventSource_release(
            final IsolateThread thread,
            final DxfgIndexedEventSourcePointer source
    ) {
        final Throwable stored = OlderEntryPoint.takeStoredException();
        final int result = EventsNative.dxfg_IndexedEventSource_release(thread, source);

        return OlderEntryPoint.failed(stored, result == ExceptionHandlerReturnMinusOne.EXECUTE_FAIL)
                ? ExceptionHandlerReturnMinusOne.EXECUTE_FAIL
                : result;
    }

    @CEntryPoint(
            name = "dxfg_v4_indexed_event_source_list_release",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_indexed_event_source_list_release(
            final IsolateThread thread,
            final DxfgIndexedEventSourceList list
    ) {
        final Throwable stored = OlderEntryPoint.takeStoredException();
        final int result = EventsNative.dxfg_CList_IndexedEventSource_release(thread, list);

        return OlderEntryPoint.failed(stored, result == ExceptionHandlerReturnMinusOne.EXECUTE_FAIL)
                ? ExceptionHandlerReturnMinusOne.EXECUTE_FAIL
                : result;
    }
}
