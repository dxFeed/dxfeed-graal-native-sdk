// Copyright (c) 2026 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

package com.dxfeed.sdk.connector;

import com.devexperts.qd.qtp.MessageConnector;
import com.dxfeed.sdk.javac.JavaObjectHandler;
import org.graalvm.nativeimage.c.CContext;
import org.graalvm.nativeimage.c.struct.CStruct;

/**
 * {@code dxfg_v4_message_connector_t}: the handle of a {@link MessageConnector}.
 */
@CContext(MessageConnectorDirectives.class)
@CStruct("dxfg_v4_message_connector_t")
public interface DxfgMessageConnectorHandle extends JavaObjectHandler<MessageConnector> {
}
