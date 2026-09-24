package com.dxfeed.sdk.model.bridge;

import com.dxfeed.api.experimental.model.TxModelListener;
import com.dxfeed.sdk.javac.JavaObjectHandler;
import org.graalvm.nativeimage.c.CContext;
import org.graalvm.nativeimage.c.struct.CStruct;

@CContext(TxModelListenerBridge.Directives.class)
@CStruct("dxfg_tx_model_listener_t")
public interface TxModelListenerCStruct extends JavaObjectHandler<TxModelListener> {
}
