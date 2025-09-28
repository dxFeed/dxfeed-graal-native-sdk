// Copyright (c) 2025 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

#pragma once

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

void getException(graal_isolatethread_t *isolateThread);

void printEvent(graal_isolatethread_t *isolateThread, const dxfg_event_type_t *event);

inline void indexedEventTxModelListenerCallback(graal_isolatethread_t *isolateThread,
                                                                   dxfg_event_type_list *events, int32_t isSnapshot,
                                                                   void */*userData*/) {
    printf("IndexedEventTxModelListenerCallback:\n");

    if (1 == isSnapshot) {
        puts("  <SNAPSHOT>");
    }

    for (int i = 0; i < events->size; ++i) {
        printEvent(isolateThread, reinterpret_cast<dxfg_event_type_t *>(events->elements[i]));
    }
}

namespace dxfg {
inline Command indexedEventTxModelCase{
    "IndexedEventTxModelCase",
    {"ietxm"},
    "",
    "ietxm [<properties>] [<address>]",
    {"ietxm %defaultAddress%"},
    [](const Command & /*self*/, graal_isolatethread_t *isolateThread, const std::vector<std::string> &args,
       const dxfg::CommandsContext &context) {
        puts("== IndexedEventTxModel ==");

        std::size_t argIndex = 0;
        auto address = dxfg::CommandLineParser::parseAddress(args, argIndex, context.getDefaultAddress());
        dxfg_endpoint_t *endpoint = dxfg_DXEndpoint_create(isolateThread);

        dxfg_DXEndpoint_connect(isolateThread, endpoint, address.c_str());

        dxfg_feed_t *feed = dxfg_DXEndpoint_getFeed(isolateThread, endpoint);
        dxfg_indexed_event_tx_model_builder_t *builder{};

        dxfg_IndexedEventTxModel_newBuilder(isolateThread, DXFG_EVENT_ORDER, &builder);
        dxfg_IndexedEventTxModel_Builder_withFeed(isolateThread, builder, feed);
        dxfg_IndexedEventTxModel_Builder_withSymbol2(isolateThread, builder, "IBM");

        dxfg_indexed_event_source_t source{.type = INDEXED_EVENT_SOURCE, .id = 6579576, .name = "dex"};

        dxfg_IndexedEventTxModel_Builder_withSource(isolateThread, builder, &source);

        dxfg_indexed_event_tx_model_listener_t *listener{};

        dxfg_IndexedEventTxModel_Listener_new(isolateThread, &indexedEventTxModelListenerCallback, nullptr, &listener);
        dxfg_IndexedEventTxModel_Builder_withListener(isolateThread, builder, nullptr);

        dxfg_indexed_event_tx_model_t *model{};

        dxfg_IndexedEventTxModel_Builder_build(isolateThread, builder, &model);

        std::this_thread::sleep_for(std::chrono::seconds(30));

        dxfg_IndexedEventTxModel_close(isolateThread, model);
        dxfg_DXEndpoint_close(isolateThread, endpoint);
        dxfg_JavaObjectHandler_release(isolateThread, &builder->handler);
        dxfg_JavaObjectHandler_release(isolateThread, &model->handler);
        dxfg_JavaObjectHandler_release(isolateThread, &listener->handler);
        dxfg_JavaObjectHandler_release(isolateThread, &feed->handler);
        dxfg_JavaObjectHandler_release(isolateThread, &endpoint->handler);
    }};
} // namespace dxfg