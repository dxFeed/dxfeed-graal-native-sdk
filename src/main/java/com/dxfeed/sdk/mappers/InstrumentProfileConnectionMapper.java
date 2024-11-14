package com.dxfeed.sdk.mappers;

import com.dxfeed.ipf.live.InstrumentProfileConnection;
import com.dxfeed.sdk.ipf.DxfgInstrumentProfileConnection;
import org.graalvm.nativeimage.c.struct.SizeOf;

public class InstrumentProfileConnectionMapper
    extends JavaObjectHandlerMapper<InstrumentProfileConnection, DxfgInstrumentProfileConnection> {

  @Override
  protected int getSizeJavaObjectHandler() {
    return SizeOf.get(DxfgInstrumentProfileConnection.class);
  }
}
