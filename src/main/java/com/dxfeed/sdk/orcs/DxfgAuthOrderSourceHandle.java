// Copyright (c) 2025 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

package com.dxfeed.sdk.orcs;

import com.dxfeed.orcs.api.AuthOrderSource;
import com.dxfeed.sdk.javac.JavaObjectHandler;
import org.graalvm.nativeimage.c.CContext;
import org.graalvm.nativeimage.c.struct.CStruct;

@CContext(Directives.class)
@CStruct("dxfg_auth_order_source_t")
public interface DxfgAuthOrderSourceHandle extends JavaObjectHandler<AuthOrderSource> {

}
