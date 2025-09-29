// Copyright (c) 2025 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

#pragma once

#include <dxfg_api.h>

#include "Common.hpp"

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

inline void indexedEventTxModelListenerCallback(graal_isolatethread_t *isolateThread, dxfg_event_type_list *events,
                                                int32_t isSnapshot, void * /*userData*/) {
    printf("IndexedEventTxModelListenerCallback:\n");

    if (1 == isSnapshot) {
        puts("  <SNAPSHOT>");
    } else {
        puts("  <UPDATE>");
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

        if (nullptr == endpoint) {
            getException(isolateThread);

            return;
        }

        finally([&] {
            dxfg_JavaObjectHandler_release(isolateThread, &endpoint->handler);
        });

        auto result = dxfg_DXEndpoint_connect(isolateThread, endpoint, address.c_str());

        if (result != DXFG_EXECUTE_SUCCESSFULLY) {
            getException(isolateThread);

            return;
        }

        finally([&] {
            dxfg_DXEndpoint_close(isolateThread, endpoint);
        });

        dxfg_feed_t *feed = dxfg_DXEndpoint_getFeed(isolateThread, endpoint);

        if (nullptr == feed) {
            getException(isolateThread);

            return;
        }

        finally([&] {
            dxfg_JavaObjectHandler_release(isolateThread, &feed->handler);
        });

        dxfg_indexed_event_tx_model_builder_t *builder{};

        result = dxfg_IndexedEventTxModel_newBuilder(isolateThread, DXFG_EVENT_ORDER, &builder);

        if (result != DXFG_EXECUTE_SUCCESSFULLY) {
            getException(isolateThread);

            return;
        }

        finally([&] {
            dxfg_JavaObjectHandler_release(isolateThread, &builder->handler);
        });

        result = dxfg_IndexedEventTxModel_Builder_withFeed(isolateThread, builder, feed);

        if (result != DXFG_EXECUTE_SUCCESSFULLY) {
            getException(isolateThread);

            return;
        }

        result = dxfg_IndexedEventTxModel_Builder_withSymbol2(isolateThread, builder, "AAPL");

        if (result != DXFG_EXECUTE_SUCCESSFULLY) {
            getException(isolateThread);

            return;
        }

        const auto source = dxfg_IndexedEventSource_new(isolateThread, "ntv");

        if (nullptr == source) {
            getException(isolateThread);

            return;
        }

        finally([&] {
            dxfg_IndexedEventSource_release(isolateThread, source);
        });

        result = dxfg_IndexedEventTxModel_Builder_withSource(isolateThread, builder, source);

        if (result != DXFG_EXECUTE_SUCCESSFULLY) {
            getException(isolateThread);

            return;
        }

        dxfg_indexed_event_tx_model_listener_t *listener{};

        result = dxfg_IndexedEventTxModel_Listener_new(isolateThread, &indexedEventTxModelListenerCallback, nullptr, &listener);

        if (result != DXFG_EXECUTE_SUCCESSFULLY) {
            getException(isolateThread);

            return;
        }

        finally([&] {
            dxfg_JavaObjectHandler_release(isolateThread, &listener->handler);
        });

        result = dxfg_IndexedEventTxModel_Builder_withListener(isolateThread, builder, listener);

        if (result != DXFG_EXECUTE_SUCCESSFULLY) {
            getException(isolateThread);

            return;
        }

        dxfg_indexed_event_tx_model_t *model{};

        result = dxfg_IndexedEventTxModel_Builder_build(isolateThread, builder, &model);

        if (result != DXFG_EXECUTE_SUCCESSFULLY) {
            getException(isolateThread);

            return;
        }

        finally([&] {
            dxfg_IndexedEventTxModel_close(isolateThread, model);
            dxfg_JavaObjectHandler_release(isolateThread, &model->handler);
        });

        std::this_thread::sleep_for(std::chrono::seconds(10));

    }};
} // namespace dxfg