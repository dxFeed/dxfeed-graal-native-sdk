package com.dxfeed.sdk.model.bridge;

import com.dxfeed.api.experimental.model.IndexedTxModel;
import com.dxfeed.sdk.mappers.JavaObjectHandlerMapper;

public final class IndexedTxModelBuilderUtils {
  public static final JavaObjectHandlerMapper<IndexedTxModel.Builder, IndexedTxModelBuilderCStruct> MAPPER = new IndexedTxModelBuilderMapper();
}
