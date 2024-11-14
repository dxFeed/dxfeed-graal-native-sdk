package com.dxfeed.sdk.mappers;

import com.dxfeed.ipf.InstrumentProfileReader;
import com.dxfeed.sdk.ipf.DxfgInstrumentProfileReader;
import org.graalvm.nativeimage.c.struct.SizeOf;

public class InstrumentProfileReaderMapper
    extends JavaObjectHandlerMapper<InstrumentProfileReader, DxfgInstrumentProfileReader> {

  @Override
  protected int getSizeJavaObjectHandler() {
    return SizeOf.get(DxfgInstrumentProfileReader.class);
  }
}
