// Copyright (c) 2026 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

package com.dxfeed.sdk.exception;

import com.oracle.svm.core.jni.JNIThreadLocalPendingException;

/**
 * Lets a v4 twin call the older entry point method as a Java method and tell whether it failed.
 *
 * <p>Called from Java, the older method still applies its own exception handler: it stores the exception in the thread
 * and returns its error value (NULL, -1, {@code INT32_MIN}), which can also be a valid result. So the twin takes the
 * stored exception away before the call and tells an error by the error value together with an exception stored during
 * the call:
 * <pre>{@code
 * final Throwable stored = OlderEntryPoint.takeStoredException();
 * final DxfgTimeZone result = JavacNative.dxfg_TimeZone_getDefault(thread);
 *
 * if (OlderEntryPoint.failed(stored, result.isNull())) {
 *     return ExceptionHandlerReturnMinusOne.EXECUTE_FAIL;
 * }
 * }</pre>
 * Exactly one call of an older method goes between {@link #takeStoredException()} and {@link #failed(Throwable,
 * boolean)}: after a successful call the earlier exception is stored again, so a second older call would be taken for
 * failed. If the older method lets the exception through instead, the twin's own handler stores it and returns -1.
 */
public final class OlderEntryPoint {

    private OlderEntryPoint() {
    }

    /**
     * Takes the exception that the thread stores (left by an earlier failed call) and clears it.
     *
     * @return The exception that the thread stored or {@code null}.
     */
    public static Throwable takeStoredException() {
        final Throwable stored = JNIThreadLocalPendingException.get();

        JNIThreadLocalPendingException.clear();

        return stored;
    }

    /**
     * Tells whether the older method failed: it returned its error value and stored an exception in the thread.
     *
     * <p>An exception stored while the older method returned another value was left by a nested call: a callback that
     * the older method ran on this thread called a function that failed. It stays stored and the call succeeds, as with
     * the older function. If no exception was stored, the exception taken before the call is stored again, so a
     * successful call does not clear it.
     *
     * @param stored        The exception that {@link #takeStoredException()} returned.
     * @param errorReturned {@code true} if the older method returned the error value of its exception handler.
     * @return {@code true} if the older method failed (its exception stays stored), {@code false} otherwise.
     */
    public static boolean failed(final Throwable stored, final boolean errorReturned) {
        if (JNIThreadLocalPendingException.get() != null) {
            return errorReturned;
        }

        if (stored != null) {
            JNIThreadLocalPendingException.set(stored);
        }

        return false;
    }
}
