// Copyright (c) 2025 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

package com.dxfeed.sdk.model;

import com.dxfeed.bridge.annotations.ListenerHandler;
import com.dxfeed.bridge.annotations.ListenerParameter;
import com.dxfeed.bridge.annotations.ParameterName;
import com.dxfeed.event.EventType;
import com.dxfeed.event.IndexedEventSource;
import com.dxfeed.sdk.NativeUtils;
import com.dxfeed.sdk.events.DxfgEventTypeList;
import com.dxfeed.sdk.source.DxfgIndexedEventSource;
import java.util.Collection;
import java.util.List;

@ListenerHandler(
        value = com.dxfeed.api.experimental.model.TxModelListener.class,
        cStruct = "dxfg_tx_model_listener_t",
        cHeader = "src/main/c/api/dxfg_event_model.h"
)
public interface TxModelListener extends com.dxfeed.api.experimental.model.TxModelListener {

    @ListenerParameter(
            value = DxfgIndexedEventSource.class,
            codeTemplateToNative = "$T.MAPPER_INDEXED_EVENT_SOURCE.toNative($N)",
            toNativeParameters = NativeUtils.class,
            codeTemplateToRelease = "$T.MAPPER_INDEXED_EVENT_SOURCE.release($N)",
            releaseParameters = NativeUtils.class,
            cTypeName = "dxfg_indexed_event_source_t*",
            cName = "source",
            order = 0
    )
    @ListenerParameter(
            value = DxfgEventTypeList.class,
            codeTemplateToNative = "$T.MAPPER_EVENTS.toNativeList(($T<? extends $T<?>>) $N)",
            toNativeParameters = {NativeUtils.class, Collection.class, EventType.class},
            codeTemplateToRelease = "$T.MAPPER_EVENTS.release($N)",
            releaseParameters = NativeUtils.class,
            cTypeName = "dxfg_event_type_list*",
            cName = "events",
            order = 1
    )
    @ParameterName(
            value = "isSnapshot",
            order = 2
    )
    @Override
    void eventsReceived(IndexedEventSource source, List events, boolean isSnapshot);
}
