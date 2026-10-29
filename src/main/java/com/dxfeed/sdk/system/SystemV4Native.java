// Copyright (c) 2026 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

package com.dxfeed.sdk.system;

import static com.dxfeed.sdk.NativeUtils.MAPPER_STRING;

import com.dxfeed.sdk.common.DxfgOut;
import com.dxfeed.sdk.exception.ExceptionHandlerReturnMinusOne;
import com.dxfeed.sdk.exception.OlderEntryPoint;
import com.dxfeed.sdk.javac.NativeListener;
import com.dxfeed.sdk.subscription.EventQueue;
import org.graalvm.nativeimage.IsolateThread;
import org.graalvm.nativeimage.c.CContext;
import org.graalvm.nativeimage.c.function.CEntryPoint;
import org.graalvm.nativeimage.c.type.CCharPointer;
import org.graalvm.nativeimage.c.type.CIntPointer;
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

    @CEntryPoint(
            name = "dxfg_v4_system_shutdown",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_system_shutdown(
            final IsolateThread ignoredThread
    ) throws InterruptedException {
        // The listeners first: no callback runs while the endpoints are closed or the queues run their tasks.
        // The queues before the endpoints: an endpoint waits until no task of a queue is pending.
        NativeListener.deactivateAll();
        try {
            EventQueue.closeAll();
        } finally {
            IsolateResources.closeAllAndAwaitTermination();
        }

        return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
    }

    @CEntryPoint(
            name = "dxfg_v4_system_get_version",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_system_get_version(
            final IsolateThread ignoredThread,
            @DxfgOut final WordPointer version
    ) {
        if (version.isNull()) {
            throw new IllegalArgumentException("The `version` pointer is null");
        }

        version.write(MAPPER_STRING.toNative(SdkFeatures.VERSION));

        return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
    }

    @CEntryPoint(
            name = "dxfg_v4_system_get_advanced_revision",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_system_get_advanced_revision(
            final IsolateThread ignoredThread,
            @DxfgOut final CIntPointer revision
    ) {
        if (revision.isNull()) {
            throw new IllegalArgumentException("The `revision` pointer is null");
        }

        revision.write(SdkFeatures.ADVANCED_REVISION);

        return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
    }

    @CEntryPoint(
            name = "dxfg_v4_system_is_feature_supported",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_system_is_feature_supported(
            final IsolateThread ignoredThread,
            final CCharPointer name,
            @DxfgOut final CIntPointer supported
    ) {
        if (name.isNull()) {
            throw new IllegalArgumentException("The `name` pointer is null");
        }

        if (supported.isNull()) {
            throw new IllegalArgumentException("The `supported` pointer is null");
        }

        supported.write(SdkFeatures.isSupported(MAPPER_STRING.toJava(name)) ? 1 : 0);

        return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
    }
}
