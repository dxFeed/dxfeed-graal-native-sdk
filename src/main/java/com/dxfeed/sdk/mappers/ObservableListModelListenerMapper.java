// Copyright (c) 2025 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

package com.dxfeed.sdk.mappers;

import com.dxfeed.event.IndexedEvent;
import com.dxfeed.model.ObservableListModelListener;
import com.dxfeed.sdk.model.DxfgObservableListModelListener;
import org.graalvm.nativeimage.c.struct.SizeOf;

public class ObservableListModelListenerMapper
        extends
        JavaObjectHandlerMapper<ObservableListModelListener<? super IndexedEvent<?>>, DxfgObservableListModelListener> {

    @Override
    protected int getSizeJavaObjectHandler() {
        return SizeOf.get(DxfgObservableListModelListener.class);
    }
}
