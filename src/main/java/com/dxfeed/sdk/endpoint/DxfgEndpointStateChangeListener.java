// Copyright (c) 2025 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

package com.dxfeed.sdk.endpoint;

import com.dxfeed.sdk.javac.JavaObjectHandler;
import java.beans.PropertyChangeListener;
import org.graalvm.nativeimage.c.CContext;
import org.graalvm.nativeimage.c.struct.CStruct;

@CContext(Directives.class)
@CStruct("dxfg_endpoint_state_change_listener_t")
public interface DxfgEndpointStateChangeListener extends JavaObjectHandler<PropertyChangeListener> {

}
