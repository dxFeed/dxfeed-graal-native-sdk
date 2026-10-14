// Copyright (c) 2026 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

package com.dxfeed.sdk.model;

import com.dxfeed.event.IndexedEvent;
import com.dxfeed.sdk.javac.JavaObjectHandler;
import com.dxfeed.sdk.model.tx.TxModelListener;
import org.graalvm.nativeimage.c.CContext;
import org.graalvm.nativeimage.c.struct.CStruct;

@CContext(ModelDirectives.class)
@CStruct("dxfg_tx_model_listener_t")
public interface DxfgTxModelListenerHandle extends JavaObjectHandler<TxModelListener<IndexedEvent<?>>> {
}
