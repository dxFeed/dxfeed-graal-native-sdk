package com.dxfeed.event.market;

import com.dxfeed.event.EventType;
import com.dxfeed.sdk.events.DxfgEventType;
import com.dxfeed.sdk.mappers.Mapper;
import org.graalvm.word.WordFactory;

public abstract class EventMapper<V extends EventType<?>, T extends DxfgEventType>
    extends Mapper<V, T> {

  public T toNative(final V jObject) {
    if (jObject == null) {
      return WordFactory.nullPointer();
    }
    final T nObject = createNativeObject();
    fillNative(jObject, nObject, true);
    return nObject;
  }

  protected abstract T createNativeObject();

  public T toNativeObjectWithCast(final EventType<?> jEvent) {
    return toNative((V) jEvent);
  }

  public V toJavaObjectWithCast(final DxfgEventType nEvent) {
    return toJava((T) nEvent);
  }

  public void fillNativeObjectWithCast(final EventType<?> jEvent, final DxfgEventType nEvent) {
    fillNative((V) jEvent, (T) nEvent, true);
  }

  public void cleanNativeObjectWithCast(final DxfgEventType nEvent) {
    cleanNative((T) nEvent);
  }

  public void fillJavaObjectWithCast(final DxfgEventType nEvent, final EventType<?> jEvent) {
    fillJava((T) nEvent, (V) jEvent);
  }

  public void releaseWithCast(final DxfgEventType nEvent) {
    release((T) nEvent);
  }

  public abstract T createNativeObject(final String symbol);
}
