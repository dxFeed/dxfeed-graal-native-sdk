// Copyright (c) 2025 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

#pragma once

#include <dxfg_api.h>

#include "CommandLineParser.hpp"
#include "CommandsContext.hpp"
#include "CommandsRegistry.hpp"

#include <chrono>
#include <cstdio>
#include <string>
#include <thread>
#include <vector>

void printEvent(graal_isolatethread_t *isolateThread, const dxfg_event_type_t *event);

inline void onDoneCallback(graal_isolatethread_t *isolateThread, dxfg_promise_t *promise, void * /* user_data */) {
    dxfg_event_type_t *event =
        dxfg_Promise_EventType_getResult(isolateThread, reinterpret_cast<dxfg_promise_event_t *>(promise));

    printf("  onDoneCallback:\n");
    printEvent(isolateThread, event);

    dxfg_EventType_release(isolateThread, event);
}

namespace dxfg {
inline Command promiseCase{"PromiseCase",
                           {"p"},
                           "",
                           "p [<properties>] [<address>]",
                           {"p %defaultAddress%"},
                           [](const Command & /*self*/, graal_isolatethread_t *isolateThread,
                              const std::vector<std::string> &args, const dxfg::CommandsContext &context) {
                               using namespace std::chrono_literals;

                               puts("== Promise ==");

                               std::size_t argIndex = 0;
                               auto address =
                                   dxfg::CommandLineParser::parseAddress(args, argIndex, context.getDefaultAddress());

                               dxfg_endpoint_t *endpoint = dxfg_DXEndpoint_create(isolateThread);

                               dxfg_DXEndpoint_connect(isolateThread, endpoint, address.c_str());

                               dxfg_feed_t *feed = dxfg_DXEndpoint_getFeed(isolateThread, endpoint);
                               dxfg_candle_symbol_t candleSymbol;

                               candleSymbol.supper.type = CANDLE;
                               candleSymbol.symbol = "AAPL";

                               dxfg_promise_event_t *candlePromise = dxfg_DXFeed_getLastEventPromise(
                                   isolateThread, feed, DXFG_EVENT_CANDLE, &candleSymbol.supper);

                               dxfg_Promise_whenDone(isolateThread, reinterpret_cast<dxfg_promise_t *>(candlePromise),
                                                     &onDoneCallback, nullptr);

                               std::this_thread::sleep_for(2s);

                               dxfg_DXEndpoint_close(isolateThread, endpoint);
                               dxfg_JavaObjectHandler_release(isolateThread, &candlePromise->handler.handler);
                               dxfg_JavaObjectHandler_release(isolateThread, &feed->handler);
                               dxfg_JavaObjectHandler_release(isolateThread, &endpoint->handler);
                           }};
} // namespace dxfg