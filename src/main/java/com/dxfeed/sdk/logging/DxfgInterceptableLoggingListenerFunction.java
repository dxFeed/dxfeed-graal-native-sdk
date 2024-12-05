// Copyright (c) 2024 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

package com.dxfeed.sdk.logging;

import com.dxfeed.sdk.exception.DxfgException;
import org.graalvm.nativeimage.IsolateThread;
import org.graalvm.nativeimage.c.CContext;
import org.graalvm.nativeimage.c.function.CFunctionPointer;
import org.graalvm.nativeimage.c.function.InvokeCFunctionPointer;
import org.graalvm.nativeimage.c.type.CCharPointer;
import org.graalvm.nativeimage.c.type.CConst;
import org.graalvm.nativeimage.c.type.CTypedef;
import org.graalvm.nativeimage.c.type.VoidPointer;

@CContext(Directives.class)
@CTypedef(name = "dxfg_logging_listener_function_t")
public interface DxfgInterceptableLoggingListenerFunction extends CFunctionPointer {

  @InvokeCFunctionPointer
  void invoke(
      final IsolateThread thread,
      final DxfgLoggingLevel loggingLevel,
      final long timestamp,
      final @CConst CCharPointer threadName,
      final long threadId,
      final @CConst CCharPointer loggerName,
      final @CConst CCharPointer message,
      final DxfgException exception,
      final @CConst CCharPointer formattedMessage,
      final VoidPointer userData
  );
}
