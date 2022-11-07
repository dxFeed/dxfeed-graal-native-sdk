package com.dxfeed.api.exception;

import com.oracle.svm.core.annotate.Uninterruptible;
import com.oracle.svm.jni.JNIThreadLocalPendingException;
import org.graalvm.nativeimage.c.function.CEntryPoint;

public class ExceptionHandlerReturnMinusOne implements CEntryPoint.ExceptionHandler {

  public static final int EXECUTE_SUCCESSFULLY = 0;
  public static final int EXECUTE_FAIL = -1;

  @Uninterruptible(reason = "exception handler")
  static int handle(final Throwable throwable) {
    JNIThreadLocalPendingException.set(throwable);
    return EXECUTE_FAIL;
  }
}
