// Copyright (c) 2025 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

package com.dxfeed.sdk.candlewebservice;

import com.dxfeed.event.TimeSeriesEvent;
import com.dxfeed.sdk.NativeUtils;
import com.dxfeed.sdk.common.DxfgOut;
import com.dxfeed.sdk.events.DxfgEventClazz;
import com.dxfeed.sdk.events.DxfgEventTypeListPointerPointer;
import com.dxfeed.sdk.exception.ExceptionHandlerReturnMinusOne;
import com.dxfeed.sdk.symbol.DxfgSymbol;
import java.io.IOException;
import org.graalvm.nativeimage.IsolateThread;
import org.graalvm.nativeimage.c.CContext;
import org.graalvm.nativeimage.c.function.CEntryPoint;

@CContext(Directives.class)
public final class HistoryEndpointNative {

    @CEntryPoint(
            name = "dxfg_HistoryEndpoint_newBuilder",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_HistoryEndpoint_newBuilder(
            final IsolateThread ignoreThread,
            @DxfgOut final DxfgHistoryEndpointBuilderHandlePointer builder
    ) {
        if (builder.isNull()) {
            throw new IllegalArgumentException("The `builder` pointer is null");
        }

        builder.write(NativeUtils.MAPPER_HISTORY_ENDPOINT_BUILDER.toNative(HistoryEndpoint.newBuilder()));
        return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
    }

    @CEntryPoint(
            name = "dxfg_HistoryEndpoint_getTimeSeries",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_HistoryEndpoint_getTimeSeries(
            final IsolateThread ignoreThread,
            final DxfgHistoryEndpointHandle historyEndpoint,
            final DxfgEventClazz dxfgClazz,
            final DxfgSymbol dxfgSymbol,
            final long fromTime,
            final long toTime,
            @DxfgOut final DxfgEventTypeListPointerPointer events

    ) throws IOException {
        if (events.isNull()) {
            throw new IllegalArgumentException("The `events` pointer is null");
        }

        //noinspection DataFlowIssue
        events.write(NativeUtils.MAPPER_EVENTS.toNativeList(
                NativeUtils.MAPPER_HISTORY_ENDPOINT.toJava(historyEndpoint).getTimeSeries(
                        (Class<TimeSeriesEvent<?>>) dxfgClazz.clazz,
                        NativeUtils.MAPPER_SYMBOL.toJava(dxfgSymbol),
                        fromTime,
                        toTime
                )));

        return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
    }
}
