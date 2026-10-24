// Copyright (c) 2026 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

package com.dxfeed.sdk.exception;

import com.dxfeed.sdk.NativeUtils;
import com.dxfeed.sdk.common.DxfgOut;
import com.oracle.svm.core.jni.JNIThreadLocalPendingException;
import org.graalvm.nativeimage.IsolateThread;
import org.graalvm.nativeimage.c.CContext;
import org.graalvm.nativeimage.c.function.CEntryPoint;
import org.graalvm.nativeimage.c.type.WordPointer;

/**
 * The v4 twins of the functions of {@link CatchExceptionNative} (see dxfg_v4_api.h).
 */
@CContext(ExceptionDirectives.class)
public final class CatchExceptionV4Native {

    private CatchExceptionV4Native() {
    }

    @CEntryPoint(
            name = "dxfg_v4_get_and_clear_thread_exception",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_get_and_clear_thread_exception(
            final IsolateThread thread,
            @DxfgOut final WordPointer exception
    ) {
        if (exception.isNull()) {
            throw new IllegalArgumentException("The `exception` pointer is null");
        }

        // Read, convert, then clear: if the conversion fails, its exception replaces the stored one (the handler).
        exception.write(NativeUtils.MAPPER_EXCEPTION.toNative(JNIThreadLocalPendingException.get()));
        JNIThreadLocalPendingException.clear();

        return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
    }

    @CEntryPoint(
            name = "dxfg_v4_Exception_release",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_Exception_release(
            final IsolateThread thread,
            final DxfgException dxfgException
    ) {
        NativeUtils.MAPPER_EXCEPTION.release(dxfgException);

        return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
    }
}
