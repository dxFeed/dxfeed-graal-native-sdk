package com.dxfeed.event.market;

import com.dxfeed.api.events.DxfgEventType;
import com.dxfeed.event.EventType;
import org.graalvm.word.WordFactory;

public abstract class EventMapper<V extends EventType<?>, T extends DxfgEventType>
    extends Mapper<V, T> {

  public T toNativeObject(final V jObject) {
    if (jObject == null) {
      return WordFactory.nullPointer();
    }
    final T nObject = createNativeObject();
    fillNativeObject(jObject, nObject);
    return nObject;
  }

  protected abstract T createNativeObject();

  public T toNativeObjectWithCast(final EventType<?> jEvent) {
    return toNativeObject((V) jEvent);
  }

  public V toJavaObjectWithCast(final DxfgEventType nEvent) {
    return toJavaObject((T) nEvent);
  }

  public void fillNativeObjectWithCast(final EventType<?> jEvent, final DxfgEventType nEvent) {
    fillNativeObject((V) jEvent, (T) nEvent);
  }

  public void cleanNativeObjectWithCast(final DxfgEventType nEvent) {
    cleanNativeObject((T) nEvent);
  }

  public void fillJavaObjectWithCast(final DxfgEventType nEvent, final EventType<?> jEvent) {
    fillJavaObject((T) nEvent, (V) jEvent);
  }

  public void releaseWithCast(final DxfgEventType nEvent) {
    release((T) nEvent);
  }

  public abstract T createNativeObject(final String symbol);
}
