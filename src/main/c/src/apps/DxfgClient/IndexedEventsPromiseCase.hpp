// Copyright (c) 2025 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

#pragma once

#include "Common.hpp"

#include <dxfg_api.h>

#include "CommandLineParser.hpp"
#include "CommandsContext.hpp"
#include "CommandsRegistry.hpp"

#include <chrono>
#include <cstdio>
#include <string>
#include <thread>
#include <vector>

namespace dxfg {
inline Command indexedEventsPromiseCase{
    "IndexedEventsPromiseCase",
    {"iep"},
    "",
    "iep [<properties>] [<address>]",
    {"iep %defaultAddress%"},
    [](const Command & /*self*/, graal_isolatethread_t *isolateThread, const std::vector<std::string> &args,
       const dxfg::CommandsContext &context) {
        using namespace std::chrono_literals;

        puts("== IndexedEventsPromise ==");

        std::size_t argIndex = 0;
        auto address = dxfg::CommandLineParser::parseAddress(args, argIndex, context.getDefaultAddress());
        dxfg_endpoint_t *endpoint = dxfg_DXEndpoint_create(isolateThread);

        dxfg_DXEndpoint_connect(isolateThread, endpoint, address.c_str());

        dxfg_feed_t *feed = dxfg_DXEndpoint_getFeed(isolateThread, endpoint);
        dxfg_string_symbol_t symbol;
        symbol.supper.type = STRING;
        symbol.symbol = "IBM";
        dxfg_indexed_event_source_t *source = dxfg_IndexedEventSource_new(isolateThread, nullptr);
        dxfg_promise_events_t *eventsPromise =
            dxfg_DXFeed_getIndexedEventsPromise(isolateThread, feed, DXFG_EVENT_SERIES, &symbol.supper, source);

        dxfg_IndexedEventSource_release(isolateThread, source);
        dxfg_Promise_awaitWithoutException(isolateThread, &eventsPromise->base, 30000);

        int hasResult = dxfg_Promise_hasResult(isolateThread, &eventsPromise->base);

        printf("  hasResult = %d\n", hasResult);

        int hasException = dxfg_Promise_hasException(isolateThread, &eventsPromise->base);

        printf("  hasException = %d\n", hasException);

        if (hasException == -1) {
            getException(isolateThread);
        } else if (hasException == 1) {
            dxfg_exception_t *exception = dxfg_Promise_getException(isolateThread, &eventsPromise->base);

            if (exception) {
                for (int i = 0; i < exception->stack_trace->size; ++i) {
                    printf("    %s\n", exception->stack_trace->elements[i]->class_name);
                }
            }

            dxfg_Exception_release(isolateThread, exception);
        } else {
            dxfg_event_type_list *events = dxfg_Promise_List_EventType_getResult(isolateThread, eventsPromise);

            printEvents(isolateThread, events, nullptr);
            dxfg_CList_EventType_release(isolateThread, events);
        }

        dxfg_DXEndpoint_close(isolateThread, endpoint);
        dxfg_JavaObjectHandler_release(isolateThread, &eventsPromise->base.handler);
        dxfg_JavaObjectHandler_release(isolateThread, &feed->handler);
        dxfg_JavaObjectHandler_release(isolateThread, &endpoint->handler);
    }};
} // namespace dxfg