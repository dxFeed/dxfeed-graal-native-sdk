package com.dxfeed.sdk.qds;

import com.devexperts.qd.QDFactory;
import com.devexperts.qd.tools.Tools;
import com.dxfeed.sdk.NativeUtils;
import com.dxfeed.sdk.exception.ExceptionHandlerReturnNullWord;
import com.dxfeed.sdk.javac.DxfgCharPointerList;
import org.graalvm.nativeimage.IsolateThread;
import org.graalvm.nativeimage.c.CContext;
import org.graalvm.nativeimage.c.function.CEntryPoint;
import org.graalvm.nativeimage.c.type.CCharPointer;

import java.util.Arrays;

@CContext(Directives.class)
public class QdsNative {

  @CEntryPoint(
      name = "dxfg_Tools_parseSymbols",
      exceptionHandler = ExceptionHandlerReturnNullWord.class
  )
  public static DxfgCharPointerList dxfg_Tools_parseSymbols(
      final IsolateThread ignoredThread,
      final CCharPointer symbolList
  )
  {
    return NativeUtils.MAPPER_STRINGS.toNativeList(
        Arrays.asList(
            Tools.parseSymbols(NativeUtils.MAPPER_STRING.toJava(symbolList), QDFactory.getDefaultScheme())
        )
    );
  }
}
