// Copyright (c) 2024 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

package com.dxfeed.sdk.logging;

import com.devexperts.logging.InterceptableLoggingListener;
import com.dxfeed.sdk.javac.JavaObjectHandler;
import org.graalvm.nativeimage.c.CContext;
import org.graalvm.nativeimage.c.struct.CStruct;

@CContext(Directives.class)
@CStruct("dxfg_logging_listener_t")
public interface DxfgInterceptableLoggingListenerHandle extends
    JavaObjectHandler<InterceptableLoggingListener> {

}
