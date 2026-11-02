// Copyright (c) 2026 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

package com.dxfeed.sdk.ondemand;

import com.dxfeed.sdk.javac.JavaObjectHandler;
import java.beans.PropertyChangeListener;
import org.graalvm.nativeimage.c.CContext;
import org.graalvm.nativeimage.c.struct.CStruct;

@CContext(OnDemandV4Directives.class)
@CStruct("dxfg_v4_on_demand_property_change_listener_t")
public interface DxfgOnDemandPropertyChangeListener extends JavaObjectHandler<PropertyChangeListener> {

}
