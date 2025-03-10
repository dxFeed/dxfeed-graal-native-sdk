// Copyright (c) 2025 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

package com.dxfeed.sdk.schedule;

import org.graalvm.nativeimage.IsolateThread;
import org.graalvm.nativeimage.c.CContext;
import org.graalvm.nativeimage.c.function.CFunctionPointer;
import org.graalvm.nativeimage.c.function.InvokeCFunctionPointer;
import org.graalvm.nativeimage.c.type.CTypedef;

@CContext(Directives.class)
@CTypedef(name = "dxfg_day_filter_function")
interface DxfgDayFilterFunction extends CFunctionPointer {

    @InvokeCFunctionPointer
    int invoke(
            final IsolateThread thread,
            final DxfgDay dxfgDay
    );
}
