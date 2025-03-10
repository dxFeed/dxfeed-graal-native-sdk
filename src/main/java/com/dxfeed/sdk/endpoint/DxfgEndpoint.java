// Copyright (c) 2025 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

package com.dxfeed.sdk.endpoint;

import com.dxfeed.api.DXEndpoint;
import com.dxfeed.sdk.javac.JavaObjectHandler;
import org.graalvm.nativeimage.c.CContext;
import org.graalvm.nativeimage.c.struct.CStruct;

/**
 * DxfgEndpoint represents a binding to a native C structure `dxfg_endpoint_t` within the context of the GraalVM native
 * image environment. It provides the ability to interface with native DXEndpoint objects.
 */
@CContext(Directives.class)
@CStruct("dxfg_endpoint_t")
public interface DxfgEndpoint extends JavaObjectHandler<DXEndpoint> {

}
