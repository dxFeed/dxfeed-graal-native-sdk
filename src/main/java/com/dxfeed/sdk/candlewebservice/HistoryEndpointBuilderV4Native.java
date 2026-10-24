// Copyright (c) 2026 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

package com.dxfeed.sdk.candlewebservice;

import com.dxfeed.sdk.common.DxfgOut;
import com.dxfeed.sdk.exception.ExceptionHandlerReturnMinusOne;
import com.dxfeed.sdk.exception.OlderEntryPoint;
import org.graalvm.nativeimage.IsolateThread;
import org.graalvm.nativeimage.c.CContext;
import org.graalvm.nativeimage.c.function.CEntryPoint;
import org.graalvm.nativeimage.c.type.CCharPointer;
import org.graalvm.nativeimage.c.type.CConst;

/**
 * The v4 twins of the functions of {@link HistoryEndpointBuilderNative}: each one calls the old function as a Java method
 * ({@link OlderEntryPoint} tells whether it failed) and returns its result through an out parameter (see
 * dxfg_v4_api.h).
 */
@CContext(CandleWebServiceDirectives.class)
public final class HistoryEndpointBuilderV4Native {

    private HistoryEndpointBuilderV4Native() {
    }

    @CEntryPoint(
            name = "dxfg_v4_HistoryEndpoint_Builder_withAddress",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_HistoryEndpoint_Builder_withAddress(
            final IsolateThread thread,
            final DxfgHistoryEndpointBuilderHandle builder,
            @CConst final CCharPointer address
    ) {
        final Throwable stored = OlderEntryPoint.takeStoredException();
        final int result = HistoryEndpointBuilderNative.dxfg_HistoryEndpoint_Builder_withAddress(
                        thread,
                        builder,
                        address);

        return OlderEntryPoint.failed(stored, result == ExceptionHandlerReturnMinusOne.EXECUTE_FAIL)
                ? ExceptionHandlerReturnMinusOne.EXECUTE_FAIL
                : result;
    }

    @CEntryPoint(
            name = "dxfg_v4_HistoryEndpoint_Builder_withUserName",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_HistoryEndpoint_Builder_withUserName(
            final IsolateThread thread,
            final DxfgHistoryEndpointBuilderHandle builder,
            @CConst final CCharPointer userName
    ) {
        final Throwable stored = OlderEntryPoint.takeStoredException();
        final int result = HistoryEndpointBuilderNative.dxfg_HistoryEndpoint_Builder_withUserName(
                        thread,
                        builder,
                        userName);

        return OlderEntryPoint.failed(stored, result == ExceptionHandlerReturnMinusOne.EXECUTE_FAIL)
                ? ExceptionHandlerReturnMinusOne.EXECUTE_FAIL
                : result;
    }

    @CEntryPoint(
            name = "dxfg_v4_HistoryEndpoint_Builder_withPassword",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_HistoryEndpoint_Builder_withPassword(
            final IsolateThread thread,
            final DxfgHistoryEndpointBuilderHandle builder,
            @CConst final CCharPointer password
    ) {
        final Throwable stored = OlderEntryPoint.takeStoredException();
        final int result = HistoryEndpointBuilderNative.dxfg_HistoryEndpoint_Builder_withPassword(
                        thread,
                        builder,
                        password);

        return OlderEntryPoint.failed(stored, result == ExceptionHandlerReturnMinusOne.EXECUTE_FAIL)
                ? ExceptionHandlerReturnMinusOne.EXECUTE_FAIL
                : result;
    }

    @CEntryPoint(
            name = "dxfg_v4_HistoryEndpoint_Builder_withAuthToken",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_HistoryEndpoint_Builder_withAuthToken(
            final IsolateThread thread,
            final DxfgHistoryEndpointBuilderHandle builder,
            @CConst final CCharPointer authToken
    ) {
        final Throwable stored = OlderEntryPoint.takeStoredException();
        final int result = HistoryEndpointBuilderNative.dxfg_HistoryEndpoint_Builder_withAuthToken(
                        thread,
                        builder,
                        authToken);

        return OlderEntryPoint.failed(stored, result == ExceptionHandlerReturnMinusOne.EXECUTE_FAIL)
                ? ExceptionHandlerReturnMinusOne.EXECUTE_FAIL
                : result;
    }

    @CEntryPoint(
            name = "dxfg_v4_HistoryEndpoint_Builder_withCompression",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_HistoryEndpoint_Builder_withCompression(
            final IsolateThread thread,
            final DxfgHistoryEndpointBuilderHandle builder,
            final DxfgHistoryEndpointCompression compression
    ) {
        final Throwable stored = OlderEntryPoint.takeStoredException();
        final int result = HistoryEndpointBuilderNative.dxfg_HistoryEndpoint_Builder_withCompression(
                        thread,
                        builder,
                        compression);

        return OlderEntryPoint.failed(stored, result == ExceptionHandlerReturnMinusOne.EXECUTE_FAIL)
                ? ExceptionHandlerReturnMinusOne.EXECUTE_FAIL
                : result;
    }

    @CEntryPoint(
            name = "dxfg_v4_HistoryEndpoint_Builder_withFormat",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_HistoryEndpoint_Builder_withFormat(
            final IsolateThread thread,
            final DxfgHistoryEndpointBuilderHandle builder,
            final DxfgHistoryEndpointFormat format
    ) {
        final Throwable stored = OlderEntryPoint.takeStoredException();
        final int result = HistoryEndpointBuilderNative.dxfg_HistoryEndpoint_Builder_withFormat(
                        thread,
                        builder,
                        format);

        return OlderEntryPoint.failed(stored, result == ExceptionHandlerReturnMinusOne.EXECUTE_FAIL)
                ? ExceptionHandlerReturnMinusOne.EXECUTE_FAIL
                : result;
    }

    @CEntryPoint(
            name = "dxfg_v4_HistoryEndpoint_Builder_build",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_HistoryEndpoint_Builder_build(
            final IsolateThread thread,
            final DxfgHistoryEndpointBuilderHandle builder,
            @DxfgOut final DxfgHistoryEndpointHandlePointer endpoint
    ) {
        final Throwable stored = OlderEntryPoint.takeStoredException();
        final int result = HistoryEndpointBuilderNative.dxfg_HistoryEndpoint_newBuilder(thread, builder, endpoint);

        return OlderEntryPoint.failed(stored, result == ExceptionHandlerReturnMinusOne.EXECUTE_FAIL)
                ? ExceptionHandlerReturnMinusOne.EXECUTE_FAIL
                : result;
    }
}
