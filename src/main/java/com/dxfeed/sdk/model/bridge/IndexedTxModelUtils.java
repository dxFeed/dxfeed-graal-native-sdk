package com.dxfeed.sdk.model.bridge;

import com.dxfeed.api.experimental.model.IndexedTxModel;
import com.dxfeed.sdk.mappers.JavaObjectHandlerMapper;

public final class IndexedTxModelUtils {
  public static final JavaObjectHandlerMapper<IndexedTxModel, IndexedTxModelCStruct> MAPPER = new IndexedTxModelMapper();
}
