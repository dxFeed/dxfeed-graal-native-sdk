package com.dxfeed.sdk.model.bridge;

import com.dxfeed.api.experimental.model.TxModelListener;
import com.dxfeed.event.IndexedEvent;
import com.dxfeed.sdk.mappers.JavaObjectHandlerMapper;
import org.graalvm.nativeimage.c.struct.SizeOf;

public class TxModelListenerMapper extends JavaObjectHandlerMapper<TxModelListener<IndexedEvent<?>>, TxModelListenerCStruct> {
  @Override
  protected int getSizeJavaObjectHandler() {
    return SizeOf.get(TxModelListenerCStruct.class);
  }
}
