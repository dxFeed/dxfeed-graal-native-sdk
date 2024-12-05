// Copyright (c) 2024 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

package com.dxfeed.sdk.logging;

import com.devexperts.logging.InterceptableLoggingListener;
import com.dxfeed.sdk.mappers.JavaObjectHandlerMapper;
import org.graalvm.nativeimage.c.struct.SizeOf;

public class InterceptableLoggingListenerMapper extends
    JavaObjectHandlerMapper<InterceptableLoggingListener, DxfgInterceptableLoggingListenerHandle> {

  @Override
  protected int getSizeJavaObjectHandler() {
    return SizeOf.get(DxfgInterceptableLoggingListenerHandle.class);
  }
}
