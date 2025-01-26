package com.dxfeed.sdk.mappers;

import com.dxfeed.ipf.InstrumentProfile;
import com.dxfeed.sdk.ipf.DxfgInstrumentProfile2ListPointer;
import com.dxfeed.sdk.ipf.DxfgInstrumentProfile2Pointer;
import com.dxfeed.sdk.ipf.DxfgInstrumentProfile2PointerPointer;
import org.graalvm.nativeimage.c.struct.SizeOf;

public class InstrumentProfile2ListMapper extends
    ListMapper<InstrumentProfile, DxfgInstrumentProfile2Pointer, DxfgInstrumentProfile2PointerPointer, DxfgInstrumentProfile2ListPointer> {

  private final Mapper<InstrumentProfile, DxfgInstrumentProfile2Pointer> mapper;

  public InstrumentProfile2ListMapper(
      final Mapper<InstrumentProfile, DxfgInstrumentProfile2Pointer> mapper) {
    this.mapper = mapper;
  }

  @Override
  protected InstrumentProfile toJava(
      final DxfgInstrumentProfile2Pointer dxfgInstrumentProfile2Pointer) {
    return this.mapper.toJava(dxfgInstrumentProfile2Pointer);
  }

  @Override
  protected DxfgInstrumentProfile2Pointer toNative(final InstrumentProfile instrumentProfile) {
    return this.mapper.toNative(instrumentProfile);
  }

  @Override
  protected void releaseNative(final DxfgInstrumentProfile2Pointer dxfgInstrumentProfile2Pointer) {
    this.mapper.release(dxfgInstrumentProfile2Pointer);
  }

  @Override
  protected int getNativeListSize() {
    return SizeOf.get(DxfgInstrumentProfile2ListPointer.class);
  }
}
