// Copyright (c) 2026 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

package com.dxfeed.sdk.logging;

import com.dxfeed.sdk.common.DxfgOut;
import com.dxfeed.sdk.exception.ExceptionHandlerReturnMinusOne;
import com.dxfeed.sdk.exception.OlderEntryPoint;
import org.graalvm.nativeimage.IsolateThread;
import org.graalvm.nativeimage.c.CContext;
import org.graalvm.nativeimage.c.function.CEntryPoint;
import org.graalvm.nativeimage.c.type.VoidPointer;

/**
 * The v4 twins of the functions of {@link LoggingNative}: each one calls the old function as a Java method
 * ({@link OlderEntryPoint} tells whether it failed) and returns its result through an out parameter (see
 * dxfg_v4_api.h).
 */
@CContext(LoggingDirectives.class)
public final class LoggingV4Native {

    private LoggingV4Native() {
    }

    @CEntryPoint(
            name = "dxfg_v4_logging_listener_new",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_logging_listener_new(
            final IsolateThread thread,
            final DxfgInterceptableLoggingListenerFunction userFunc,
            final VoidPointer userData,
            @DxfgOut final DxfgInterceptableLoggingListenerHandlePointer listener
    ) {
        final Throwable stored = OlderEntryPoint.takeStoredException();
        final int result = LoggingNative.DxfgInterceptableLoggingListenerNew(thread, userFunc, userData, listener);

        return OlderEntryPoint.failed(stored, result == ExceptionHandlerReturnMinusOne.EXECUTE_FAIL)
                ? ExceptionHandlerReturnMinusOne.EXECUTE_FAIL
                : result;
    }

    @CEntryPoint(
            name = "dxfg_v4_logging_set_listener",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_logging_set_listener(
            final IsolateThread thread,
            final DxfgInterceptableLoggingListenerHandle listener
    ) {
        final Throwable stored = OlderEntryPoint.takeStoredException();
        final int result = LoggingNative.DxfgInterceptableLoggingSetListener(thread, listener);

        return OlderEntryPoint.failed(stored, result == ExceptionHandlerReturnMinusOne.EXECUTE_FAIL)
                ? ExceptionHandlerReturnMinusOne.EXECUTE_FAIL
                : result;
    }

    @CEntryPoint(
            name = "dxfg_v4_logging_set_log_level",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_logging_set_log_level(
            final IsolateThread thread,
            final DxfgLoggingLevel level
    ) {
        final Throwable stored = OlderEntryPoint.takeStoredException();
        final int result = LoggingNative.DxfgInterceptableLoggingSetLogLevel(thread, level);

        return OlderEntryPoint.failed(stored, result == ExceptionHandlerReturnMinusOne.EXECUTE_FAIL)
                ? ExceptionHandlerReturnMinusOne.EXECUTE_FAIL
                : result;
    }

    @CEntryPoint(
            name = "dxfg_v4_logging_set_err_level",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_logging_set_err_level(
            final IsolateThread thread,
            final DxfgLoggingLevel level
    ) {
        final Throwable stored = OlderEntryPoint.takeStoredException();
        final int result = LoggingNative.DxfgInterceptableLoggingSetErrLevel(thread, level);

        return OlderEntryPoint.failed(stored, result == ExceptionHandlerReturnMinusOne.EXECUTE_FAIL)
                ? ExceptionHandlerReturnMinusOne.EXECUTE_FAIL
                : result;
    }
}
