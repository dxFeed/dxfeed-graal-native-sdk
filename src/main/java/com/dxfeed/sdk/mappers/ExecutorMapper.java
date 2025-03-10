// Copyright (c) 2025 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

package com.dxfeed.sdk.mappers;

import com.dxfeed.sdk.javac.DxfgExecutor;
import java.util.concurrent.Executor;
import org.graalvm.nativeimage.c.struct.SizeOf;

public class ExecutorMapper extends JavaObjectHandlerMapper<Executor, DxfgExecutor> {

    @Override
    protected int getSizeJavaObjectHandler() {
        return SizeOf.get(DxfgExecutor.class);
    }
}
