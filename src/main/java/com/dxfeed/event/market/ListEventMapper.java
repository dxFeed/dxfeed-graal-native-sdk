package com.dxfeed.event.market;

import com.dxfeed.event.EventType;
import com.dxfeed.sdk.events.DxfgEventType;
import com.dxfeed.sdk.events.DxfgEventTypeList;
import com.dxfeed.sdk.events.DxfgEventTypePointer;
import com.dxfeed.sdk.mappers.ListMapper;
import com.dxfeed.sdk.mappers.Mapper;
import org.graalvm.nativeimage.c.struct.SizeOf;

public class ListEventMapper extends
    ListMapper<EventType<?>, DxfgEventType, DxfgEventTypePointer, DxfgEventTypeList> {

  private final Mapper<EventType<?>, DxfgEventType> eventMappers;

  public ListEventMapper(final Mapper<EventType<?>, DxfgEventType> eventMappers) {
    this.eventMappers = eventMappers;
  }

  @Override
  protected EventType<?> toJava(final DxfgEventType nObject) {
    return eventMappers.toJava(nObject);
  }

  @Override
  protected void releaseNative(final DxfgEventType nObject) {
    eventMappers.release(nObject);
  }

  @Override
  protected DxfgEventType toNative(final EventType<?> jObject) {
    return eventMappers.toNative(jObject);
  }

  @Override
  protected int getNativeListSize() {
    return SizeOf.get(DxfgEventTypeList.class);
  }
}
