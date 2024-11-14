package com.dxfeed.sdk.mappers;

import com.dxfeed.ipf.live.InstrumentProfileCollector;
import com.dxfeed.sdk.ipf.DxfgInstrumentProfileCollector;
import org.graalvm.nativeimage.c.struct.SizeOf;

public class InstrumentProfileCollectorMapper
    extends JavaObjectHandlerMapper<InstrumentProfileCollector, DxfgInstrumentProfileCollector> {

  @Override
  protected int getSizeJavaObjectHandler() {
    return SizeOf.get(DxfgInstrumentProfileCollector.class);
  }
}
