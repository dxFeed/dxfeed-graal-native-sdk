package com.dxfeed.event.market;

import com.dxfeed.api.BaseNative;
import com.dxfeed.api.events.DxfgMarketEvent;
import org.graalvm.nativeimage.UnmanagedMemory;

public abstract class MarketEventNativeMapper
    <V extends MarketEvent, T extends DxfgMarketEvent>
    extends BaseNative
    implements NativeMapper<V, T> {

  @Override
  public final T nativeObject(final V qdEvent) {
    final T nativeEvent = createNativeEvent(qdEvent);
    nativeEvent.setEventSymbol(toCString(qdEvent.getEventSymbol()));
    nativeEvent.setEventTime(qdEvent.getEventTime());
    return nativeEvent;
  }

  @Override
  public void delete(final T nativeEvent) {
    UnmanagedMemory.free(nativeEvent.getEventSymbol());
    doDelete(nativeEvent);
  }

  protected abstract T createNativeEvent(final V qdEvent);

  protected abstract void doDelete(final T nativeEvent);
}
