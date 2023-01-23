package com.dxfeed.api.maper;

import com.dxfeed.api.ipf.DxfgInstrumentProfileCollector;
import com.dxfeed.ipf.live.InstrumentProfileCollector;
import org.graalvm.nativeimage.c.struct.SizeOf;

public class InstrumentProfileCollectorMapper
    extends JavaObjectHandlerMapper<InstrumentProfileCollector, DxfgInstrumentProfileCollector> {

  @Override
  protected int getSizeJavaObjectHandler() {
    return SizeOf.get(DxfgInstrumentProfileCollector.class);
  }
}
