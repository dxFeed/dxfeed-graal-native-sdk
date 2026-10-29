// Copyright (c) 2025 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

package com.dxfeed.sdk.mappers;

import com.dxfeed.sdk.javac.DxfgExecutorHandle;
import com.dxfeed.sdk.subscription.EventQueue;
import java.util.concurrent.Executor;
import org.graalvm.nativeimage.c.struct.SizeOf;

/**
 * Every native function that accepts an executor maps it here, so the executor of a subscription attached to an event
 * queue is refused in one place ({@link EventQueue#checkExecutor}).
 */
public class ExecutorMapper extends JavaObjectHandlerMapper<Executor, DxfgExecutorHandle> {

    @Override
    protected Executor doToJava(final DxfgExecutorHandle nativeObject) {
        return EventQueue.checkExecutor(toJavaOfAnyKind(nativeObject));
    }

    /**
     * Maps the handle without the check of {@link #doToJava}, for a function that checks the executor's class itself.
     */
    public Executor toJavaOfAnyKind(final DxfgExecutorHandle nativeObject) {
        return nativeObject.isNull() ? null : super.doToJava(nativeObject);
    }

    @Override
    protected int getSizeJavaObjectHandler() {
        return SizeOf.get(DxfgExecutorHandle.class);
    }
}
