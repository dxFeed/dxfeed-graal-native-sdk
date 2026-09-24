package com.dxfeed.sdk.model.bridge;

import com.dxfeed.api.experimental.model.TimeSeriesTxModel;
import com.dxfeed.sdk.javac.JavaObjectHandler;
import org.graalvm.nativeimage.c.CContext;
import org.graalvm.nativeimage.c.struct.CStruct;

@CContext(TimeSeriesTxModelBridge.Directives.class)
@CStruct("dxfg_time_series_tx_model_t")
public interface TimeSeriesTxModelCStruct extends JavaObjectHandler<TimeSeriesTxModel> {
}
