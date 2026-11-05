// Copyright (c) 2026 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

package com.dxfeed.sdk.connector;

import com.dxfeed.sdk.javac.DxfgJavaObjectHandlerList;
import org.graalvm.nativeimage.c.CContext;
import org.graalvm.nativeimage.c.struct.CStruct;

/**
 * {@code dxfg_v4_message_connector_list}: a list of connector handles. Its layout is that of
 * {@code dxfg_java_object_handler_list}, so the list mapper of the handles creates and releases it.
 */
@CContext(MessageConnectorDirectives.class)
@CStruct("dxfg_v4_message_connector_list")
public interface DxfgMessageConnectorList extends DxfgJavaObjectHandlerList {
}
