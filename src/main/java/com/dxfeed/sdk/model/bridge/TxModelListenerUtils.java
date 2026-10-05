package com.dxfeed.sdk.model.bridge;

import com.dxfeed.api.experimental.model.TxModelListener;
import com.dxfeed.event.IndexedEvent;
import com.dxfeed.sdk.mappers.JavaObjectHandlerMapper;

public final class TxModelListenerUtils {
  public static final JavaObjectHandlerMapper<TxModelListener<IndexedEvent<?>>, TxModelListenerCStruct> MAPPER = new TxModelListenerMapper();

  /**
   * The listener for a model of the events of type {@code E}. The listeners of dxfg_TxModelListener_new take the
   * events of any type (they pass them to the C code), so they fit a model of any events.
   */
  @SuppressWarnings("unchecked")
  public static <E extends IndexedEvent<?>> TxModelListener<E> forModel(final TxModelListener<IndexedEvent<?>> listener) {
    return (TxModelListener<E>) (TxModelListener<?>) listener;
  }
}
