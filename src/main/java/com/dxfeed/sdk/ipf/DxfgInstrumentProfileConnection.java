// Copyright (c) 2025 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

package com.dxfeed.sdk.ipf;

import com.dxfeed.ipf.live.InstrumentProfileConnection;
import com.dxfeed.sdk.javac.JavaObjectHandler;
import org.graalvm.nativeimage.c.CContext;
import org.graalvm.nativeimage.c.struct.CStruct;

@CContext(Directives.class)
@CStruct("dxfg_ipf_connection_t")
public interface DxfgInstrumentProfileConnection extends JavaObjectHandler<InstrumentProfileConnection> {

}
