// Copyright (c) 2026 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

package com.dxfeed.sdk.schedule;

import org.graalvm.nativeimage.IsolateThread;
import org.graalvm.nativeimage.c.CContext;
import org.graalvm.nativeimage.c.function.CFunctionPointer;
import org.graalvm.nativeimage.c.function.InvokeCFunctionPointer;
import org.graalvm.nativeimage.c.type.CTypedef;
import org.graalvm.nativeimage.c.type.VoidPointer;

@CContext(ScheduleDirectives.class)
@CTypedef(name = "dxfg_v4_day_filter_user_data_function")
interface DxfgDayFilterUserDataFunction extends CFunctionPointer {

    @InvokeCFunctionPointer
    int invoke(
            final IsolateThread thread,
            final DxfgDay day,
            final VoidPointer userData
    );
}
