package com.dxfeed.sdk.model.bridge;

import com.dxfeed.api.experimental.model.TxModelListener;
import com.dxfeed.sdk.mappers.JavaObjectHandlerMapper;

public final class TxModelListenerUtils {
  public static final JavaObjectHandlerMapper<TxModelListener, TxModelListenerCStruct> MAPPER = new TxModelListenerMapper();
}
