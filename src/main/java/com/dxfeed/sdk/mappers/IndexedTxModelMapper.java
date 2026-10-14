// Copyright (c) 2026 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

package com.dxfeed.sdk.mappers;

import com.dxfeed.sdk.model.DxfgIndexedTxModelHandle;
import com.dxfeed.sdk.model.tx.IndexedTxModel;
import org.graalvm.nativeimage.c.struct.SizeOf;

public class IndexedTxModelMapper
        extends JavaObjectHandlerMapper<IndexedTxModel<?>, DxfgIndexedTxModelHandle> {

    @Override
    protected int getSizeJavaObjectHandler() {
        return SizeOf.get(DxfgIndexedTxModelHandle.class);
    }
}
