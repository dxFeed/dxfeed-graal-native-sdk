// Copyright (c) 2026 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

package com.dxfeed.sdk.mappers;

import com.dxfeed.sdk.ondemand.DxfgOnDemandPropertyChangeListener;
import java.beans.PropertyChangeListener;
import org.graalvm.nativeimage.c.struct.SizeOf;

public class OnDemandPropertyChangeListenerMapper
        extends JavaObjectHandlerMapper<PropertyChangeListener, DxfgOnDemandPropertyChangeListener> {

    @Override
    protected int getSizeJavaObjectHandler() {
        return SizeOf.get(DxfgOnDemandPropertyChangeListener.class);
    }
}
