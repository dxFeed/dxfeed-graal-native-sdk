// Copyright (c) 2025 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

package com.dxfeed.sdk.mappers;

import com.dxfeed.sdk.ipf.DxfgIpfConnectionStateChangeListener;
import java.beans.PropertyChangeListener;
import org.graalvm.nativeimage.c.struct.SizeOf;

public class IpfConnectionStateChangeListenerMapper
        extends JavaObjectHandlerMapper<PropertyChangeListener, DxfgIpfConnectionStateChangeListener> {

    @Override
    protected int getSizeJavaObjectHandler() {
        return SizeOf.get(DxfgIpfConnectionStateChangeListener.class);
    }
}
