// Copyright (c) 2025 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

package com.dxfeed.sdk.mappers;

import com.dxfeed.event.IndexedEvent;
import com.dxfeed.model.ObservableListModel;
import com.dxfeed.sdk.model.DxfgObservableListModel;
import org.graalvm.nativeimage.c.struct.SizeOf;

public class ObservableListModelMapper
        extends JavaObjectHandlerMapper<ObservableListModel<? extends IndexedEvent<?>>, DxfgObservableListModel> {

    @Override
    protected int getSizeJavaObjectHandler() {
        return SizeOf.get(DxfgObservableListModel.class);
    }
}
