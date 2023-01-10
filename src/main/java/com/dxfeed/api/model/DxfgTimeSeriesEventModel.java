package com.dxfeed.api.model;

import com.dxfeed.api.javac.JavaObjectHandler;
import com.dxfeed.model.TimeSeriesEventModel;
import org.graalvm.nativeimage.c.CContext;
import org.graalvm.nativeimage.c.struct.CStruct;

@CContext(Directives.class)
@CStruct("dxfg_time_series_event_model_t")
public interface DxfgTimeSeriesEventModel extends JavaObjectHandler<TimeSeriesEventModel<?>> {

}
