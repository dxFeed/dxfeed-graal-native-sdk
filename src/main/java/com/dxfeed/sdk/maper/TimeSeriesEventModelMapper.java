package com.dxfeed.sdk.maper;

import com.dxfeed.model.TimeSeriesEventModel;
import com.dxfeed.sdk.model.DxfgTimeSeriesEventModel;
import org.graalvm.nativeimage.c.struct.SizeOf;

public class TimeSeriesEventModelMapper
    extends JavaObjectHandlerMapper<TimeSeriesEventModel<?>, DxfgTimeSeriesEventModel> {

  @Override
  protected int getSizeJavaObjectHandler() {
    return SizeOf.get(DxfgTimeSeriesEventModel.class);
  }
}
