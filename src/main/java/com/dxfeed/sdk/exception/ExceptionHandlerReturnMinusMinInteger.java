package com.dxfeed.sdk.exception;

import com.oracle.svm.core.Uninterruptible;
import com.oracle.svm.core.jni.JNIThreadLocalPendingException;
import org.graalvm.nativeimage.c.function.CEntryPoint;

public class ExceptionHandlerReturnMinusMinInteger implements CEntryPoint.ExceptionHandler {

  @Uninterruptible(reason = "exception handler")
  static int handle(final Throwable throwable) {
    JNIThreadLocalPendingException.set(throwable);
    return ExceptionHandlerReturnMinusOne.EXECUTE_FAIL_MIN;
  }
}
