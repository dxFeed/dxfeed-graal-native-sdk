// Copyright (c) 2024 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

package com.dxfeed.sdk.common;

import com.dxfeed.sdk.NativeUtils;
import com.dxfeed.sdk.exception.ExceptionHandlerReturnMinusOne;
import org.graalvm.nativeimage.IsolateThread;
import org.graalvm.nativeimage.UnmanagedMemory;
import org.graalvm.nativeimage.c.CContext;
import org.graalvm.nativeimage.c.function.CEntryPoint;
import org.graalvm.nativeimage.c.type.VoidPointer;

@CContext(Directives.class)
public class CommonNative {

  @CEntryPoint(
      name = "dxfg_free_string_to_double_map_entries",
      exceptionHandler = ExceptionHandlerReturnMinusOne.class
  )
  public static int dxfgFreeStringToDoubleMapEntries(final IsolateThread ignoredThread,
      final DxfgStringToDoubleMapEntryPointer mapEntries, int size) {
    if (mapEntries.isNonNull() && size > 0) {
      for (int i = 0; i < size; i++) {
        NativeUtils.MAPPER_STRING.release(mapEntries.addressOf(i).getKey());
      }

      UnmanagedMemory.free(mapEntries);
    }

    return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
  }

  @CEntryPoint(
      name = "dxfg_free",
      exceptionHandler = ExceptionHandlerReturnMinusOne.class
  )
  public static int dxfgFree(final IsolateThread ignoredThread,
      final VoidPointer pointer) {
    UnmanagedMemory.free(pointer);

    return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
  }

}
