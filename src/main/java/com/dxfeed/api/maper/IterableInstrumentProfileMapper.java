package com.dxfeed.api.maper;

import com.dxfeed.api.ipf.DxfgIterableInstrumentProfile;
import com.dxfeed.ipf.InstrumentProfile;
import java.util.Iterator;
import org.graalvm.nativeimage.c.struct.SizeOf;

public class IterableInstrumentProfileMapper
    extends JavaObjectHandlerMapper<Iterator<InstrumentProfile>, DxfgIterableInstrumentProfile> {

  @Override
  protected int getSizeJavaObjectHandler() {
    return SizeOf.get(DxfgIterableInstrumentProfile.class);
  }
}
