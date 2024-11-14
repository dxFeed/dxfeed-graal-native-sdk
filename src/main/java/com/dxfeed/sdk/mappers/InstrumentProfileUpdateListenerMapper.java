package com.dxfeed.sdk.mappers;

import com.dxfeed.ipf.live.InstrumentProfileUpdateListener;
import com.dxfeed.sdk.ipf.DxfgInstrumentProfileUpdateListener;
import org.graalvm.nativeimage.c.struct.SizeOf;

public class InstrumentProfileUpdateListenerMapper
    extends JavaObjectHandlerMapper<InstrumentProfileUpdateListener, DxfgInstrumentProfileUpdateListener> {

  @Override
  protected int getSizeJavaObjectHandler() {
    return SizeOf.get(DxfgInstrumentProfileUpdateListener.class);
  }
}
