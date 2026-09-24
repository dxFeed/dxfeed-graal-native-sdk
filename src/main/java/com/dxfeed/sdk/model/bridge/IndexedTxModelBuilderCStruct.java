package com.dxfeed.sdk.model.bridge;

import com.dxfeed.api.experimental.model.IndexedTxModel;
import com.dxfeed.sdk.javac.JavaObjectHandler;
import org.graalvm.nativeimage.c.CContext;
import org.graalvm.nativeimage.c.struct.CStruct;

@CContext(IndexedTxModelBuilderBridge.Directives.class)
@CStruct("dxfg_indexed_tx_model_builder_t")
public interface IndexedTxModelBuilderCStruct extends JavaObjectHandler<IndexedTxModel.Builder> {
}
