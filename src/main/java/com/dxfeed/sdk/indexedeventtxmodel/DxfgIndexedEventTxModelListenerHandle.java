// Copyright (c) 2025 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

package com.dxfeed.sdk.indexedeventtxmodel;

import com.dxfeed.model.IndexedEventTxModel;
import com.dxfeed.sdk.javac.JavaObjectHandler;
import org.graalvm.nativeimage.c.CContext;
import org.graalvm.nativeimage.c.struct.CStruct;

@SuppressWarnings("rawtypes")
@CContext(Directives.class)
@CStruct("dxfg_indexed_event_tx_model_listener_t")
public interface DxfgIndexedEventTxModelListenerHandle extends JavaObjectHandler<IndexedEventTxModel.Listener> {

}
