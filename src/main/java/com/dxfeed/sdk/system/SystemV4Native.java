// Copyright (c) 2026 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

package com.dxfeed.sdk.system;

import com.dxfeed.sdk.common.DxfgOut;
import com.dxfeed.sdk.exception.ExceptionHandlerReturnMinusOne;
import com.dxfeed.sdk.exception.OlderEntryPoint;
import org.graalvm.nativeimage.IsolateThread;
import org.graalvm.nativeimage.c.CContext;
import org.graalvm.nativeimage.c.function.CEntryPoint;
import org.graalvm.nativeimage.c.type.CCharPointer;
import org.graalvm.nativeimage.c.type.WordPointer;

/**
 * The v4 twins of the functions of {@link SystemNative}: each one calls the old function as a Java method
 * ({@link OlderEntryPoint} tells whether it failed) and returns its result through an out parameter (see
 * dxfg_v4_api.h).
 */
@CContext(SystemDirectives.class)
public final class SystemV4Native {

    private SystemV4Native() {
    }

    @CEntryPoint(
            name = "dxfg_v4_system_set_property",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_system_set_property(
            final IsolateThread thread,
            final CCharPointer key,
            final CCharPointer value
    ) {
        final Throwable stored = OlderEntryPoint.takeStoredException();
        final int result = SystemNative.setSystemProperty(thread, key, value);

        return OlderEntryPoint.failed(stored, result == ExceptionHandlerReturnMinusOne.EXECUTE_FAIL)
                ? ExceptionHandlerReturnMinusOne.EXECUTE_FAIL
                : result;
    }

    @CEntryPoint(
            name = "dxfg_v4_system_get_property",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_system_get_property(
            final IsolateThread thread,
            final CCharPointer key,
            @DxfgOut final WordPointer value
    ) {
        if (value.isNull()) {
            throw new IllegalArgumentException("The `value` pointer is null");
        }

        final Throwable stored = OlderEntryPoint.takeStoredException();
        final CCharPointer result = SystemNative.getSystemProperty(thread, key);

        if (OlderEntryPoint.failed(stored, result.isNull())) {
            return ExceptionHandlerReturnMinusOne.EXECUTE_FAIL;
        }

        value.write(result);

        return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
    }

    @CEntryPoint(
            name = "dxfg_v4_system_release_property",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_system_release_property(
            final IsolateThread thread,
            final CCharPointer value
    ) {
        final Throwable stored = OlderEntryPoint.takeStoredException();
        final int result = SystemNative.releaseSystemProperty(thread, value);

        return OlderEntryPoint.failed(stored, result == ExceptionHandlerReturnMinusOne.EXECUTE_FAIL)
                ? ExceptionHandlerReturnMinusOne.EXECUTE_FAIL
                : result;
    }

    @CEntryPoint(
            name = "dxfg_v4_system_close_all_and_await_termination",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_system_close_all_and_await_termination(
            final IsolateThread thread
    ) throws InterruptedException {
        final Throwable stored = OlderEntryPoint.takeStoredException();
        final int result = SystemNative.closeAllAndAwaitTermination(thread);

        return OlderEntryPoint.failed(stored, result == ExceptionHandlerReturnMinusOne.EXECUTE_FAIL)
                ? ExceptionHandlerReturnMinusOne.EXECUTE_FAIL
                : result;
    }
}
