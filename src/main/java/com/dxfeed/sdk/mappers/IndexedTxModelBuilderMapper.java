// Copyright (c) 2026 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

package com.dxfeed.sdk.mappers;

import com.dxfeed.sdk.model.DxfgIndexedTxModelBuilderHandle;
import com.dxfeed.sdk.model.tx.IndexedTxModel;
import org.graalvm.nativeimage.c.struct.SizeOf;

public class IndexedTxModelBuilderMapper
        extends JavaObjectHandlerMapper<IndexedTxModel.Builder<?>, DxfgIndexedTxModelBuilderHandle> {

    @Override
    protected int getSizeJavaObjectHandler() {
        return SizeOf.get(DxfgIndexedTxModelBuilderHandle.class);
    }
}
