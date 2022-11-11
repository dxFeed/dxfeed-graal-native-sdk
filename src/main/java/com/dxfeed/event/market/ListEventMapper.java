package com.dxfeed.event.market;

import com.dxfeed.api.events.DxfgEventPointer;
import com.dxfeed.event.EventType;
import java.util.List;
import org.graalvm.nativeimage.UnmanagedMemory;
import org.graalvm.nativeimage.c.struct.SizeOf;

public class ListEventMapper implements ListMapper<EventType<?>, DxfgEventPointer> {

  protected final EventMappers eventMappers;

  public ListEventMapper(final EventMappers eventMappers) {
    this.eventMappers = eventMappers;
  }


  @Override
  public DxfgEventPointer nativeObject(final List<EventType<?>> events) {
    final DxfgEventPointer nativeEvents = UnmanagedMemory.calloc(
        SizeOf.get(DxfgEventPointer.class) * events.size()
    );
    for (int i = 0; i < events.size(); ++i) {
      nativeEvents.addressOf(i).write(eventMappers.nativeObject(events.get(i)));
    }
    return nativeEvents;
  }

  @Override
  public void delete(final DxfgEventPointer nativeEvents, final int size) {
    for (int i = 0; i < size; ++i) {
      eventMappers.delete(nativeEvents.addressOf(i).read());
    }
    UnmanagedMemory.free(nativeEvents);
  }
}
