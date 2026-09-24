package com.dxfeed.sdk.model.bridge;

import com.dxfeed.api.experimental.model.TimeSeriesTxModel;
import com.dxfeed.sdk.javac.JavaObjectHandler;
import org.graalvm.nativeimage.c.CContext;
import org.graalvm.nativeimage.c.struct.CStruct;

@CContext(TimeSeriesTxModelBuilderBridge.Directives.class)
@CStruct("dxfg_time_series_tx_model_builder_t")
public interface TimeSeriesTxModelBuilderCStruct extends JavaObjectHandler<TimeSeriesTxModel.Builder> {
}
