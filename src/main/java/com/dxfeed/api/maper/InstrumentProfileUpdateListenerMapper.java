package com.dxfeed.api.maper;

import com.dxfeed.api.ipf.DxfgInstrumentProfileUpdateListener;
import com.dxfeed.ipf.live.InstrumentProfileUpdateListener;
import org.graalvm.nativeimage.c.struct.SizeOf;

public class InstrumentProfileUpdateListenerMapper
    extends JavaObjectHandlerMapper<InstrumentProfileUpdateListener, DxfgInstrumentProfileUpdateListener> {

  @Override
  protected int getSizeJavaObjectHandler() {
    return SizeOf.get(DxfgInstrumentProfileUpdateListener.class);
  }
}
