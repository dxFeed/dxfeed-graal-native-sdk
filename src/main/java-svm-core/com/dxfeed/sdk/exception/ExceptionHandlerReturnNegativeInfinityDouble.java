// Copyright (c) 2025 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

package com.dxfeed.sdk.exception;

import com.oracle.svm.core.Uninterruptible;
import com.oracle.svm.core.jni.JNIThreadLocalPendingException;
import org.graalvm.nativeimage.c.function.CEntryPoint;

public class ExceptionHandlerReturnNegativeInfinityDouble implements CEntryPoint.ExceptionHandler {

    @Uninterruptible(reason = "exception handler")
    static double handle(final Throwable throwable) {
        JNIThreadLocalPendingException.set(throwable);
        return ExceptionHandlerReturnMinusOne.EXECUTE_FAIL_NEGATIVE_INFINITY;
    }
}
