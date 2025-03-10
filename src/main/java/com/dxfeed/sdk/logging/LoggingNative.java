// Copyright (c) 2024 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

package com.dxfeed.sdk.logging;

import com.devexperts.logging.InterceptableLogging;
import com.devexperts.logging.InterceptableLoggingListener;
import com.dxfeed.sdk.NativeUtils;
import com.dxfeed.sdk.common.DxfgOut;
import com.dxfeed.sdk.exception.ExceptionHandlerReturnMinusOne;
import java.util.logging.Level;
import org.graalvm.nativeimage.CurrentIsolate;
import org.graalvm.nativeimage.IsolateThread;
import org.graalvm.nativeimage.c.CContext;
import org.graalvm.nativeimage.c.function.CEntryPoint;
import org.graalvm.nativeimage.c.type.VoidPointer;

@CContext(Directives.class)
public class LoggingNative {

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

    listener.write(NativeUtils.MAPPER_INTERCEPTABLE_LOGGING_LISTENER.toNative(
        new InterceptableLoggingListener() {
          @Override
          public void onLog(final Level level, final long timestamp, final String threadName,
              final long threadId, final String loggerName, final String message,
              final Throwable exception, final String formattedMessage) {
            var nativeThreadName = NativeUtils.MAPPER_STRING.toNative(threadName);
            var nativeLoggerName = NativeUtils.MAPPER_STRING.toNative(loggerName);
            var nativeMessage = NativeUtils.MAPPER_STRING.toNative(message);
            var nativeException = NativeUtils.MAPPER_EXCEPTION.toNative(exception);
            var naiveFormattedMessage = NativeUtils.MAPPER_STRING.toNative(formattedMessage);

            userFunc.invoke(CurrentIsolate.getCurrentThread(), DxfgLoggingLevel.of(level),
                timestamp, nativeThreadName, threadId, nativeLoggerName, nativeMessage,
                nativeException, naiveFormattedMessage, userData);

            NativeUtils.MAPPER_STRING.release(naiveFormattedMessage);
            NativeUtils.MAPPER_EXCEPTION.release(nativeException);
            NativeUtils.MAPPER_STRING.release(nativeMessage);
            NativeUtils.MAPPER_STRING.release(nativeLoggerName);
            NativeUtils.MAPPER_STRING.release(nativeThreadName);
          }
        }));

    return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
  }

  @CEntryPoint(
      name = "dxfg_logging_set_listener",
      exceptionHandler = ExceptionHandlerReturnMinusOne.class
  )
  public static int DxfgInterceptableLoggingSetListener(final IsolateThread ignoredThread, final DxfgInterceptableLoggingListenerHandle listener) {
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
  public static int DxfgInterceptableLoggingSetLogLevel(final IsolateThread ignoredThread, final DxfgLoggingLevel level) {
    InterceptableLogging.setLogLevel(level.getLevel());

    return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
  }

  @CEntryPoint(
      name = "dxfg_logging_set_err_level",
      exceptionHandler = ExceptionHandlerReturnMinusOne.class
  )
  public static int DxfgInterceptableLoggingSetErrLevel(final IsolateThread ignoredThread, final DxfgLoggingLevel level) {
    InterceptableLogging.setErrLevel(level.getLevel());

    return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
  }

}
