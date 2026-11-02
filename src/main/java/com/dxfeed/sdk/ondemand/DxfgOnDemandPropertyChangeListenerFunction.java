// Copyright (c) 2026 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

package com.dxfeed.sdk.ondemand;

import org.graalvm.nativeimage.IsolateThread;
import org.graalvm.nativeimage.c.CContext;
import org.graalvm.nativeimage.c.function.CFunctionPointer;
import org.graalvm.nativeimage.c.function.InvokeCFunctionPointer;
import org.graalvm.nativeimage.c.type.CCharPointer;
import org.graalvm.nativeimage.c.type.CTypedef;
import org.graalvm.nativeimage.c.type.VoidPointer;

@CContext(OnDemandV4Directives.class)
@CTypedef(name = "dxfg_v4_OnDemandPropertyChangeListener_function")
public interface DxfgOnDemandPropertyChangeListenerFunction extends CFunctionPointer {

    @InvokeCFunctionPointer
    void invoke(
            final IsolateThread thread,
            final CCharPointer propertyName,
            final DxfgOnDemandPropertyValue oldValue,
            final DxfgOnDemandPropertyValue newValue,
            final VoidPointer userData
    );
}
