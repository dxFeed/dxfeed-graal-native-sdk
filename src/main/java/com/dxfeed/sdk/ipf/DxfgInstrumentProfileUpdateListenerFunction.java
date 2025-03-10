// Copyright (c) 2025 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

package com.dxfeed.sdk.ipf;

import org.graalvm.nativeimage.IsolateThread;
import org.graalvm.nativeimage.c.CContext;
import org.graalvm.nativeimage.c.function.CFunctionPointer;
import org.graalvm.nativeimage.c.function.InvokeCFunctionPointer;
import org.graalvm.nativeimage.c.type.CTypedef;
import org.graalvm.nativeimage.c.type.VoidPointer;

@CContext(Directives.class)
@CTypedef(name = "dxfg_ipf_update_listener_function")
interface DxfgInstrumentProfileUpdateListenerFunction extends CFunctionPointer {

    @InvokeCFunctionPointer
    void invoke(
            final IsolateThread thread,
            final DxfgIterableInstrumentProfile dxfgIterableInstrumentProfile,
            final VoidPointer userData
    );
}
