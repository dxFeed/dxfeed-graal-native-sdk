package com.dxfeed.sdk.mappers;

import com.dxfeed.ipf.InstrumentProfileCustomFields;
import com.dxfeed.sdk.ipf.DxfgInstrumentProfileCustomFieldsHandle;
import org.graalvm.nativeimage.c.struct.SizeOf;

public class InstrumentProfileCustomFieldsMapper extends JavaObjectHandlerMapper<InstrumentProfileCustomFields, DxfgInstrumentProfileCustomFieldsHandle> {

  @Override
  protected int getSizeJavaObjectHandler() {
    return SizeOf.get(DxfgInstrumentProfileCustomFieldsHandle.class);
  }
}
