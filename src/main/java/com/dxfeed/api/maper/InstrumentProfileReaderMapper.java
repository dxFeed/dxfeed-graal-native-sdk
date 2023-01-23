package com.dxfeed.api.maper;

import com.dxfeed.api.ipf.DxfgInstrumentProfileReader;
import com.dxfeed.ipf.InstrumentProfileReader;
import org.graalvm.nativeimage.c.struct.SizeOf;

public class InstrumentProfileReaderMapper
    extends JavaObjectHandlerMapper<InstrumentProfileReader, DxfgInstrumentProfileReader> {

  @Override
  protected int getSizeJavaObjectHandler() {
    return SizeOf.get(DxfgInstrumentProfileReader.class);
  }
}
