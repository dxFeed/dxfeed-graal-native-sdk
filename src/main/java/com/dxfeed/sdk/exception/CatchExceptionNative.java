// Copyright (c) 2025 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

package com.dxfeed.sdk.exception;

import com.dxfeed.sdk.NativeUtils;
import com.oracle.svm.core.jni.JNIThreadLocalPendingException;
import org.graalvm.nativeimage.IsolateThread;
import org.graalvm.nativeimage.c.CContext;
import org.graalvm.nativeimage.c.function.CEntryPoint;

@CContext(Directives.class)
public final class CatchExceptionNative {

    @CEntryPoint(
            name = "dxfg_get_and_clear_thread_exception_t",
            exceptionHandler = ExceptionHandlerReturnNullWord.class
    )
    public static DxfgException getAndClearThreadException(final IsolateThread ignoredThread) {
        final DxfgException dxfgException = NativeUtils.MAPPER_EXCEPTION.toNative(
                JNIThreadLocalPendingException.get()
        );
        JNIThreadLocalPendingException.clear();
        return dxfgException;
    }

    @CEntryPoint(name = "dxfg_Exception_release")
    public static void releaseException(
            final IsolateThread ignoredThread,
            final DxfgException dxfgException
    ) {
        NativeUtils.MAPPER_EXCEPTION.release(dxfgException);
    }
}
