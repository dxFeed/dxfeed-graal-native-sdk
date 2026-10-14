// Copyright (c) 2026 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

package com.dxfeed.sdk.mappers;

import com.dxfeed.event.IndexedEvent;
import com.dxfeed.sdk.model.DxfgTxModelListenerHandle;
import com.dxfeed.sdk.model.tx.TxModelListener;
import org.graalvm.nativeimage.c.struct.SizeOf;

public class TxModelListenerMapper
        extends JavaObjectHandlerMapper<TxModelListener<IndexedEvent<?>>, DxfgTxModelListenerHandle> {

    @Override
    protected int getSizeJavaObjectHandler() {
        return SizeOf.get(DxfgTxModelListenerHandle.class);
    }
}
