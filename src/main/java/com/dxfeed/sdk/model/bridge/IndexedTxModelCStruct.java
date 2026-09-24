package com.dxfeed.sdk.model.bridge;

import com.dxfeed.api.experimental.model.IndexedTxModel;
import com.dxfeed.sdk.javac.JavaObjectHandler;
import org.graalvm.nativeimage.c.CContext;
import org.graalvm.nativeimage.c.struct.CStruct;

@CContext(IndexedTxModelBridge.Directives.class)
@CStruct("dxfg_indexed_tx_model_t")
public interface IndexedTxModelCStruct extends JavaObjectHandler<IndexedTxModel> {
}
