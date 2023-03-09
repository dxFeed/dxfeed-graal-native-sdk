package com.dxfeed.sdk.exception;

import com.oracle.svm.core.Uninterruptible;
import com.oracle.svm.core.jni.JNIThreadLocalPendingException;
import org.graalvm.nativeimage.c.function.CEntryPoint;

public class ExceptionHandlerReturnMinusOneLong implements CEntryPoint.ExceptionHandler {

  @Uninterruptible(reason = "exception handler")
  static long handle(final Throwable throwable) {
    JNIThreadLocalPendingException.set(throwable);
    return ExceptionHandlerReturnMinusOne.EXECUTE_FAIL;
  }
}
