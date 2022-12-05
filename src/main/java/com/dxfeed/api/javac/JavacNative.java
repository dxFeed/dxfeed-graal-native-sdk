package com.dxfeed.api.javac;

import static com.dxfeed.api.NativeUtils.MAPPER_JAVA_OBJECT_HANDLER;
import static com.dxfeed.api.NativeUtils.MAPPER_JAVA_OBJECT_HANDLERS;
import static com.dxfeed.api.exception.ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;

import com.dxfeed.api.exception.ExceptionHandlerReturnMinusOne;
import org.graalvm.nativeimage.IsolateThread;
import org.graalvm.nativeimage.c.CContext;
import org.graalvm.nativeimage.c.function.CEntryPoint;

@CContext(Directives.class)
public class JavacNative {

  @CEntryPoint(
      name = "dxfg_JavaObjectHandler_release",
      exceptionHandler = ExceptionHandlerReturnMinusOne.class
  )
  public static int dxfg_JavaObjectHandler_release(
      final IsolateThread ignoredThread,
      final JavaObjectHandler<Object> javaObjectHandler
  ) {
    MAPPER_JAVA_OBJECT_HANDLER.release(javaObjectHandler);
    return EXECUTE_SUCCESSFULLY;
  }

  @CEntryPoint(
      name = "dxfg_CList_JavaObjectHandler_release",
      exceptionHandler = ExceptionHandlerReturnMinusOne.class
  )
  public static int dxfg_CList_JavaObjectHandler_release(
      final IsolateThread ignoredThread,
      final DxfgJavaObjectHandlerList cList
  ) {
    MAPPER_JAVA_OBJECT_HANDLERS.release(cList);
    return EXECUTE_SUCCESSFULLY;
  }
}
