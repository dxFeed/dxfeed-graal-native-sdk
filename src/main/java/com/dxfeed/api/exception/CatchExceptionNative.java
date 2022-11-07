package com.dxfeed.api.exception;

import com.dxfeed.api.BaseNative;
import com.oracle.svm.jni.JNIThreadLocalPendingException;
import java.io.PrintWriter;
import java.io.StringWriter;
import org.graalvm.nativeimage.IsolateThread;
import org.graalvm.nativeimage.c.CContext;
import org.graalvm.nativeimage.c.function.CEntryPoint;
import org.graalvm.word.WordFactory;

@CContext(Directives.class)
public final class CatchExceptionNative extends BaseNative {

  @CEntryPoint(name = "dxfg_get_and_clear_thread_exception_t")
  public static void getAndClearThreadException(
      final IsolateThread ignoredThread,
      final DxfgException dxfgException
  ) {
    final Throwable throwable = JNIThreadLocalPendingException.get();
    if (throwable != null) {
      dxfgException.setClassName(toCString(throwable.getClass().getCanonicalName()));
      dxfgException.setMessage(toCString(throwable.getMessage()));
      final StringWriter sw = new StringWriter();
      throwable.printStackTrace(new PrintWriter(sw));
      dxfgException.setStackTrace(toCString(sw.toString()));
    } else {
      dxfgException.setClassName(WordFactory.nullPointer());
      dxfgException.setMessage(WordFactory.nullPointer());
      dxfgException.setStackTrace(WordFactory.nullPointer());
    }
    JNIThreadLocalPendingException.clear();
  }

}
