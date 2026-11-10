// Copyright (c) 2025 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

package com.dxfeed.sdk.logging;

import com.devexperts.logging.InterceptableLogging;
import com.devexperts.logging.InterceptableLoggingListener;
import com.dxfeed.sdk.NativeUtils;
import com.dxfeed.sdk.common.DxfgOut;
import com.dxfeed.sdk.exception.ExceptionHandlerReturnMinusOne;
import com.dxfeed.sdk.javac.NativeListener;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import org.graalvm.nativeimage.CurrentIsolate;
import org.graalvm.nativeimage.IsolateThread;
import org.graalvm.nativeimage.c.CContext;
import org.graalvm.nativeimage.c.function.CEntryPoint;
import org.graalvm.nativeimage.c.type.VoidPointer;

@CContext(LoggingDirectives.class)
public class LoggingNative {

    private static final AtomicBoolean LISTENER_FAILURE_REPORTED = new AtomicBoolean();

    // Reports to stderr the first failure of a logging listener call, once per isolate: the logging cannot report it.
    private static void reportListenerFailure(final Throwable failure) {
        if (LISTENER_FAILURE_REPORTED.compareAndSet(false, true)) {
            System.err.println("dxfg: a logging listener call failed, its record is dropped (reported once): "
                    + failure);
        }
    }

    @CEntryPoint(
            name = "dxfg_logging_listener_new",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int DxfgInterceptableLoggingListenerNew(
            final IsolateThread ignoredThread,
            final DxfgInterceptableLoggingListenerFunction userFunc,
            final VoidPointer userData,
            @DxfgOut final DxfgInterceptableLoggingListenerHandlePointer listener

    ) {
        if (listener.isNull()) {
            throw new IllegalArgumentException("The `listener` pointer is null.");
        }

        final class Listener extends NativeListener implements InterceptableLoggingListener {
            @Override
            protected boolean ignoresMutedScope() {
                return true;
            }

            @Override
            public void onLog(final Level level, final long timestamp, final String threadName,
                    final long threadId, final String loggerName, final String message,
                    final Throwable exception, final String formattedMessage) {
                if (!enter()) {
                    return;
                }
                try {
                    var nativeThreadName = NativeUtils.MAPPER_STRING.toNative(threadName);
                    var nativeLoggerName = NativeUtils.MAPPER_STRING.toNative(loggerName);
                    var nativeMessage = NativeUtils.MAPPER_STRING.toNative(message);
                    var nativeException = NativeUtils.MAPPER_EXCEPTION.toNative(exception);
                    var nativeFormattedMessage = NativeUtils.MAPPER_STRING.toNative(formattedMessage);

                    try {
                        userFunc.invoke(CurrentIsolate.getCurrentThread(), DxfgLoggingLevel.of(level),
                                timestamp, nativeThreadName, threadId, nativeLoggerName, nativeMessage,
                                nativeException, nativeFormattedMessage, userData);
                    } finally {
                        NativeUtils.MAPPER_STRING.release(nativeFormattedMessage);
                        NativeUtils.MAPPER_EXCEPTION.release(nativeException);
                        NativeUtils.MAPPER_STRING.release(nativeMessage);
                        NativeUtils.MAPPER_STRING.release(nativeLoggerName);
                        NativeUtils.MAPPER_STRING.release(nativeThreadName);
                    }
                } catch (final Throwable t) {
                    // The record is lost, but the code that logged goes on: Logger.log() must not throw into it.
                    // Not logged: that would call this listener again.
                    reportListenerFailure(t);
                } finally {
                    exit();
                }
            }
        }

        listener.write(NativeUtils.MAPPER_INTERCEPTABLE_LOGGING_LISTENER.toNative(new Listener()));

        return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
    }

    @CEntryPoint(
            name = "dxfg_logging_set_listener",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int DxfgInterceptableLoggingSetListener(final IsolateThread ignoredThread,
            final DxfgInterceptableLoggingListenerHandle listener) {
        var javaListener = NativeUtils.MAPPER_INTERCEPTABLE_LOGGING_LISTENER.toJava(listener);

        if (javaListener != null) {
            InterceptableLogging.setListener(javaListener);
        }

        return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
    }

    @CEntryPoint(
            name = "dxfg_logging_set_log_level",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int DxfgInterceptableLoggingSetLogLevel(final IsolateThread ignoredThread,
            final DxfgLoggingLevel level) {
        InterceptableLogging.setLogLevel(level.getLevel());

        return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
    }

    @CEntryPoint(
            name = "dxfg_logging_set_err_level",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int DxfgInterceptableLoggingSetErrLevel(final IsolateThread ignoredThread,
            final DxfgLoggingLevel level) {
        InterceptableLogging.setErrLevel(level.getLevel());

        return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
    }

}
