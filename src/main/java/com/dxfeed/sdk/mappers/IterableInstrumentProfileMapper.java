package com.dxfeed.sdk.mappers;

import com.dxfeed.ipf.InstrumentProfile;
import com.dxfeed.sdk.ipf.DxfgIterableInstrumentProfile;
import java.util.Iterator;
import org.graalvm.nativeimage.c.struct.SizeOf;

public class IterableInstrumentProfileMapper
    extends JavaObjectHandlerMapper<Iterator<InstrumentProfile>, DxfgIterableInstrumentProfile> {

  @Override
  protected int getSizeJavaObjectHandler() {
    return SizeOf.get(DxfgIterableInstrumentProfile.class);
  }
}
