// Copyright (c) 2025 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

#pragma once

#include "Common.hpp"

#include <dxfg_api.h>

#include "CommandLineParser.hpp"
#include "CommandsContext.hpp"
#include "CommandsRegistry.hpp"

#include <chrono>
#include <cstdint>
#include <cstdio>
#include <iostream>
#include <string>
#include <thread>
#include <vector>

namespace dxfg {

inline void txModelListenerCallback(graal_isolatethread_t *isolateThread, dxfg_indexed_event_source_t * /* source */,
                                    dxfg_event_type_list *events, int32_t /* isSnapshot */, void * /* user_data */) {
    printf("txModelListenerCallback:\n");

    for (int i = 0; i < events->size; ++i) {
        printEvent(isolateThread, reinterpret_cast<dxfg_event_type_t *>(events->elements[i]));
    }
}

inline Command txIndexedEventModelCase{
    "TxIndexedEventModelCase",
    {"txiem"},
    "",
    "txiem [<properties>] [<address>]",
    {"txiem %defaultAddress%"},
    [](const Command & /*self*/, graal_isolatethread_t *isolateThread, const std::vector<std::string> &args,
       const dxfg::CommandsContext &context) {
        puts("== TxIndexedEventModel ==");

        std::size_t argIndex = 0;
        auto address = dxfg::CommandLineParser::parseAddress(args, argIndex, context.getDefaultAddress());
        dxfg_endpoint_t *endpoint = dxfg_DXEndpoint_create(isolateThread);

        dxfg_DXEndpoint_connect(isolateThread, endpoint, address.c_str());

        dxfg_feed_t *feed = dxfg_DXEndpoint_getFeed(isolateThread, endpoint);
        dxfg_string_symbol_t symbol;
        symbol.supper.type = STRING;
        symbol.symbol = "IBM";

        dxfg_indexed_tx_model_builder_t *builder = dxfg_IndexedTxModel_newBuilder(isolateThread, DXFG_EVENT_ORDER);
        dxfg_IndexedTxModel_Builder_withBatchProcessing(isolateThread, builder, 1);
        dxfg_IndexedTxModel_Builder_withSnapshotProcessing(isolateThread, builder, 1);
        dxfg_IndexedTxModel_Builder_withFeed(isolateThread, builder, feed);
        dxfg_IndexedTxModel_Builder_withSymbol(isolateThread, builder, &symbol.supper);
        dxfg_tx_model_listener_t *listener = dxfg_TxModelListener_new(isolateThread, &txModelListenerCallback, nullptr);
        dxfg_IndexedTxModel_Builder_withListener(isolateThread, builder, listener);
        dxfg_indexed_tx_model_t *model = dxfg_IndexedTxModel_Builder_build(isolateThread, builder);

        std::this_thread::sleep_for(std::chrono::seconds(2));

        dxfg_indexed_event_source_list sourceList;
        sourceList.size = 1;
        sourceList.elements = new dxfg_indexed_event_source_t *[sourceList.size] {
        };

        dxfg_indexed_event_source_t source;
        source.id = 6579576;
        source.name = "dex";
        source.type = INDEXED_EVENT_SOURCE;

        for (int i = 0; i < sourceList.size; i++) {
            sourceList.elements[i] = &source;
        }

        dxfg_IndexedTxModel_setSources(isolateThread, model, &sourceList);

        delete[] sourceList.elements;

        std::this_thread::sleep_for(std::chrono::seconds(2));

        dxfg_IndexedTxModel_close(isolateThread, model);
        dxfg_DXEndpoint_close(isolateThread, endpoint);
        dxfg_JavaObjectHandler_release(isolateThread, &builder->handler);
        dxfg_JavaObjectHandler_release(isolateThread, &model->handler);
        dxfg_JavaObjectHandler_release(isolateThread, &listener->handler);
        dxfg_JavaObjectHandler_release(isolateThread, &feed->handler);
        dxfg_JavaObjectHandler_release(isolateThread, &endpoint->handler);
    }};
} // namespace dxfg