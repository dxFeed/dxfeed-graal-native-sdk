package com.dxfeed.event.market;

import com.dxfeed.event.EventType;
import com.dxfeed.sdk.events.DxfgEventClazz;
import com.dxfeed.sdk.events.DxfgEventClazzList;
import com.dxfeed.sdk.events.DxfgEventClazzPointer;
import com.dxfeed.sdk.maper.ListMapper;
import org.graalvm.nativeimage.UnmanagedMemory;
import org.graalvm.nativeimage.c.struct.SizeOf;
import org.graalvm.nativeimage.c.type.CIntPointer;

public class ListEventTypeMapper extends
    ListMapper<Class<? extends EventType<?>>, CIntPointer, DxfgEventClazzPointer, DxfgEventClazzList> {

  @Override
  protected Class<? extends EventType<?>> toJava(final CIntPointer nObject) {
    return DxfgEventClazz.fromCValue(nObject.read()).clazz;
  }

  @Override
  protected CIntPointer toNative(final Class<? extends EventType<?>> jObject) {
    final CIntPointer cIntPointer = UnmanagedMemory.calloc(SIZE_OF_C_POINTER);
    cIntPointer.write(DxfgEventClazz.of(jObject).getCValue());
    return cIntPointer;
  }

  @Override
  protected void releaseNative(final CIntPointer nObject) {
    UnmanagedMemory.free(nObject);
  }

  @Override
  protected int getSizeCList() {
    return SizeOf.get(DxfgEventClazzList.class);
  }
}
