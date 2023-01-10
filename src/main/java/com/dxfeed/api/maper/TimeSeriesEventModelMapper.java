package com.dxfeed.api.maper;

import com.dxfeed.api.model.DxfgTimeSeriesEventModel;
import com.dxfeed.model.TimeSeriesEventModel;
import org.graalvm.nativeimage.c.struct.SizeOf;

public class TimeSeriesEventModelMapper
    extends JavaObjectHandlerMapper<TimeSeriesEventModel<?>, DxfgTimeSeriesEventModel> {

  @Override
  protected int getSizeJavaObjectHandler() {
    return SizeOf.get(DxfgTimeSeriesEventModel.class);
  }
}
