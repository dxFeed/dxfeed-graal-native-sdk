package com.dxfeed.api.utils;

import com.dxfeed.api.BaseNative;
import org.graalvm.nativeimage.IsolateThread;
import org.graalvm.nativeimage.UnmanagedMemory;
import org.graalvm.nativeimage.c.CContext;
import org.graalvm.nativeimage.c.function.CEntryPoint;
import org.graalvm.word.PointerBase;

@CContext(Directives.class)
public final class UtilsNative extends BaseNative {

  @CEntryPoint(name = "dxfg_utils_free")
  public static void free(final IsolateThread ignoredThread, final PointerBase pointer) {
    if (pointer.isNonNull()) {
      UnmanagedMemory.free(pointer);
    }
  }
}
