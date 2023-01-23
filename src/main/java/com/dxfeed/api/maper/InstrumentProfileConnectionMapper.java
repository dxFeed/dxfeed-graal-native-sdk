package com.dxfeed.api.maper;

import com.dxfeed.api.ipf.DxfgInstrumentProfileConnection;
import com.dxfeed.ipf.live.InstrumentProfileConnection;
import org.graalvm.nativeimage.c.struct.SizeOf;

public class InstrumentProfileConnectionMapper
    extends JavaObjectHandlerMapper<InstrumentProfileConnection, DxfgInstrumentProfileConnection> {

  @Override
  protected int getSizeJavaObjectHandler() {
    return SizeOf.get(DxfgInstrumentProfileConnection.class);
  }
}
