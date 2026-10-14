// Copyright (c) 2026 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

package com.dxfeed.sdk.model;

import com.dxfeed.sdk.events.DxfgEventTypeListPointer;
import com.dxfeed.sdk.source.DxfgIndexedEventSourcePointer;
import org.graalvm.nativeimage.IsolateThread;
import org.graalvm.nativeimage.c.CContext;
import org.graalvm.nativeimage.c.function.CFunctionPointer;
import org.graalvm.nativeimage.c.function.InvokeCFunctionPointer;
import org.graalvm.nativeimage.c.type.CTypedef;
import org.graalvm.nativeimage.c.type.VoidPointer;

@CContext(ModelDirectives.class)
@CTypedef(name = "dxfg_TxModelListener_function_eventsReceived")
public interface DxfgTxModelListenerEventsReceivedFunction extends CFunctionPointer {

    @InvokeCFunctionPointer
    void invoke(
            final IsolateThread thread,
            final DxfgIndexedEventSourcePointer source,
            final DxfgEventTypeListPointer events,
            final int isSnapshot /* bool */,
            final VoidPointer userData
    );
}
