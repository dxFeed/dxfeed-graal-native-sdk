// Copyright (c) 2025 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

package com.dxfeed.sdk.mappers;

import com.dxfeed.sdk.javac.DxfgExecutorHandle;
import java.util.concurrent.Executor;
import org.graalvm.nativeimage.c.struct.SizeOf;

public class ExecutorMapper extends JavaObjectHandlerMapper<Executor, DxfgExecutorHandle> {

    @Override
    protected int getSizeJavaObjectHandler() {
        return SizeOf.get(DxfgExecutorHandle.class);
    }
}
