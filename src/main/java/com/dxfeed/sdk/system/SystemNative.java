package com.dxfeed.sdk.system;

import static com.dxfeed.sdk.NativeUtils.MAPPER_STRING;
import static com.dxfeed.sdk.exception.ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;

import com.dxfeed.sdk.exception.ExceptionHandlerReturnMinusOne;
import com.dxfeed.sdk.exception.ExceptionHandlerReturnNullWord;
import org.graalvm.nativeimage.IsolateThread;
import org.graalvm.nativeimage.c.CContext;
import org.graalvm.nativeimage.c.function.CEntryPoint;
import org.graalvm.nativeimage.c.type.CCharPointer;

@CContext(Directives.class)
public final class SystemNative {

  @CEntryPoint(
      name = "dxfg_system_set_property",
      exceptionHandler = ExceptionHandlerReturnMinusOne.class
  )
  public static int setSystemProperty(
      final IsolateThread ignoredThread,
      final CCharPointer key,
      final CCharPointer value
  ) {
    System.setProperty(
        MAPPER_STRING.toJava(key),
        MAPPER_STRING.toJava(value)
    );
    return EXECUTE_SUCCESSFULLY;
  }

  @CEntryPoint(
      name = "dxfg_system_get_property",
      exceptionHandler = ExceptionHandlerReturnNullWord.class
  )
  public static CCharPointer getSystemProperty(
      final IsolateThread ignoredThread,
      final CCharPointer key
  ) {
    return MAPPER_STRING.toNative(
        System.getProperty(MAPPER_STRING.toJava(key))
    );
  }

  @CEntryPoint(
      name = "dxfg_system_release_property",
      exceptionHandler = ExceptionHandlerReturnMinusOne.class
  )
  public static int releaseSystemProperty(
      final IsolateThread ignoredThread,
      final CCharPointer value
  ) {
    MAPPER_STRING.release(value);
    return EXECUTE_SUCCESSFULLY;
  }
}
