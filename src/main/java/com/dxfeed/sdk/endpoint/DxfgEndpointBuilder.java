// Copyright (c) 2025 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

package com.dxfeed.sdk.endpoint;

import com.dxfeed.api.DXEndpoint;
import com.dxfeed.sdk.javac.JavaObjectHandler;
import org.graalvm.nativeimage.c.CContext;
import org.graalvm.nativeimage.c.struct.CStruct;

@CContext(Directives.class)
@CStruct("dxfg_endpoint_builder_t")
public interface DxfgEndpointBuilder extends JavaObjectHandler<DXEndpoint.Builder> {

}
