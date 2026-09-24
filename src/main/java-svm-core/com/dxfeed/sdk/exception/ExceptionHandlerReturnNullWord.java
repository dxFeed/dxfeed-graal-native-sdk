// Copyright (c) 2025 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

package com.dxfeed.sdk.exception;

import com.oracle.svm.core.Uninterruptible;
import com.oracle.svm.core.jni.JNIThreadLocalPendingException;
import org.graalvm.nativeimage.c.function.CEntryPoint;
import org.graalvm.word.PointerBase;
import org.graalvm.word.WordFactory;

public class ExceptionHandlerReturnNullWord implements CEntryPoint.ExceptionHandler {

    @Uninterruptible(reason = "exception handler")
    static <T extends PointerBase> T handle(final Throwable throwable) {
        JNIThreadLocalPendingException.set(throwable);
        return WordFactory.nullPointer();
    }
}
