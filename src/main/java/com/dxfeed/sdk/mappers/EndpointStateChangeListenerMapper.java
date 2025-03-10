// Copyright (c) 2025 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

package com.dxfeed.sdk.mappers;

import com.dxfeed.sdk.endpoint.DxfgEndpointStateChangeListener;
import java.beans.PropertyChangeListener;
import org.graalvm.nativeimage.c.struct.SizeOf;

public class EndpointStateChangeListenerMapper
        extends JavaObjectHandlerMapper<PropertyChangeListener, DxfgEndpointStateChangeListener> {

    @Override
    protected int getSizeJavaObjectHandler() {
        return SizeOf.get(DxfgEndpointStateChangeListener.class);
    }
}
