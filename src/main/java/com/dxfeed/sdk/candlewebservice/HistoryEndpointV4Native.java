// Copyright (c) 2026 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

package com.dxfeed.sdk.candlewebservice;

import com.dxfeed.sdk.common.DxfgOut;
import com.dxfeed.sdk.events.DxfgEventClazz;
import com.dxfeed.sdk.events.DxfgEventTypeListPointerPointer;
import com.dxfeed.sdk.exception.ExceptionHandlerReturnMinusOne;
import com.dxfeed.sdk.exception.OlderEntryPoint;
import com.dxfeed.sdk.symbol.DxfgSymbol;
import java.io.IOException;
import org.graalvm.nativeimage.IsolateThread;
import org.graalvm.nativeimage.c.CContext;
import org.graalvm.nativeimage.c.function.CEntryPoint;

/**
 * The v4 twins of the functions of {@link HistoryEndpointNative}: each one calls the old function as a Java method
 * ({@link OlderEntryPoint} tells whether it failed) and returns its result through an out parameter (see
 * dxfg_v4_api.h).
 */
@CContext(CandleWebServiceDirectives.class)
public final class HistoryEndpointV4Native {

    private HistoryEndpointV4Native() {
    }

    @CEntryPoint(
            name = "dxfg_v4_HistoryEndpoint_newBuilder",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_HistoryEndpoint_newBuilder(
            final IsolateThread thread,
            @DxfgOut final DxfgHistoryEndpointBuilderHandlePointer builder
    ) {
        final Throwable stored = OlderEntryPoint.takeStoredException();
        final int result = HistoryEndpointNative.dxfg_HistoryEndpoint_newBuilder(thread, builder);

        return OlderEntryPoint.failed(stored, result == ExceptionHandlerReturnMinusOne.EXECUTE_FAIL)
                ? ExceptionHandlerReturnMinusOne.EXECUTE_FAIL
                : result;
    }

    @CEntryPoint(
            name = "dxfg_v4_HistoryEndpoint_getTimeSeries",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_HistoryEndpoint_getTimeSeries(
            final IsolateThread thread,
            final DxfgHistoryEndpointHandle historyEndpoint,
            final DxfgEventClazz dxfgClazz,
            final DxfgSymbol dxfgSymbol,
            final long fromTime,
            final long toTime,
            @DxfgOut final DxfgEventTypeListPointerPointer events
    ) throws IOException {
        final Throwable stored = OlderEntryPoint.takeStoredException();
        final int result = HistoryEndpointNative.dxfg_HistoryEndpoint_getTimeSeries(
                        thread,
                        historyEndpoint,
                        dxfgClazz,
                        dxfgSymbol,
                        fromTime,
                        toTime,
                        events);

        return OlderEntryPoint.failed(stored, result == ExceptionHandlerReturnMinusOne.EXECUTE_FAIL)
                ? ExceptionHandlerReturnMinusOne.EXECUTE_FAIL
                : result;
    }
}
