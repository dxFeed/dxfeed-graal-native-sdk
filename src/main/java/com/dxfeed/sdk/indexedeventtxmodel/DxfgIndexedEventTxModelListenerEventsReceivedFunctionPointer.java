// Copyright (c) 2025 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

package com.dxfeed.sdk.indexedeventtxmodel;

import com.dxfeed.sdk.events.DxfgEventTypeListPointer;
import org.graalvm.nativeimage.IsolateThread;
import org.graalvm.nativeimage.c.CContext;
import org.graalvm.nativeimage.c.function.CFunctionPointer;
import org.graalvm.nativeimage.c.function.InvokeCFunctionPointer;
import org.graalvm.nativeimage.c.type.CTypedef;
import org.graalvm.nativeimage.c.type.VoidPointer;

@SuppressWarnings("rawtypes")
@CContext(Directives.class)
@CTypedef(
        name = "dxfg_IndexedEventTxModel_Listener_eventsReceived_f"
)
public interface DxfgIndexedEventTxModelListenerEventsReceivedFunctionPointer extends CFunctionPointer {
    @InvokeCFunctionPointer
    int invoke(IsolateThread thread, DxfgEventTypeListPointer events,
            int isSnapshot /* bool */, VoidPointer userData);
}
