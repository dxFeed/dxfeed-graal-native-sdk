// Copyright (c) 2025 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

package com.dxfeed.sdk.indexedeventtxmodel;

import com.dxfeed.model.IndexedEventTxModel;
import com.dxfeed.sdk.mappers.JavaObjectHandlerMapper;
import org.graalvm.nativeimage.c.struct.SizeOf;

@SuppressWarnings("rawtypes")
public class IndexedEventTxModelListenerMapper extends
        JavaObjectHandlerMapper<IndexedEventTxModel.Listener, DxfgIndexedEventTxModelListenerHandle> {

    @Override
    protected int getSizeJavaObjectHandler() {
        return SizeOf.get(DxfgIndexedEventTxModelListenerHandle.class);
    }
}
