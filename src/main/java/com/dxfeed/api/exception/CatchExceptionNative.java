package com.dxfeed.api.exception;

import com.dxfeed.api.BaseNative;
import com.dxfeed.event.market.ExceptionMapper;
import com.dxfeed.event.market.StringMapper;
import com.oracle.svm.jni.JNIThreadLocalPendingException;
import org.graalvm.nativeimage.IsolateThread;
import org.graalvm.nativeimage.c.CContext;
import org.graalvm.nativeimage.c.function.CEntryPoint;

@CContext(Directives.class)
public final class CatchExceptionNative extends BaseNative {

  private static final ExceptionMapper EXCEPTION_MAPPER = new ExceptionMapper(new StringMapper());

  @CEntryPoint(
      name = "dxfg_get_and_clear_thread_exception_t",
      exceptionHandler = ExceptionHandlerReturnNullWord.class
  )
  public static DxfgException getAndClearThreadException(final IsolateThread ignoredThread) {
    return EXCEPTION_MAPPER.nativeObject(
        JNIThreadLocalPendingException.get()
    );
  }

  @CEntryPoint(name = "dxfg_release_exception_t")
  public static void releaseException(
      final IsolateThread ignoredThread,
      final DxfgException dxfgException
  ) {
    EXCEPTION_MAPPER.delete(dxfgException);
    JNIThreadLocalPendingException.clear();
  }
}
