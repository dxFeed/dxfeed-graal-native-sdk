package com.dxfeed.api.system;

import static com.dxfeed.api.NativeUtils.MAPPER_STRING;
import static com.dxfeed.api.exception.ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;

import com.dxfeed.api.exception.ExceptionHandlerReturnMinusOne;
import com.dxfeed.api.exception.ExceptionHandlerReturnNullWord;
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
        MAPPER_STRING.toJavaObject(key),
        MAPPER_STRING.toJavaObject(value)
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
    return MAPPER_STRING.toNativeObject(
        System.getProperty(MAPPER_STRING.toJavaObject(key))
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
