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

namespace dxfg {
inline Command lastEventIfSubscribedCase{
    "LastEventIfSubscribedCase",
    {"le"},
    "",
    "le [<properties>] [<address>]",
    {"le %defaultAddress%"},
    [](const Command & /*self*/, graal_isolatethread_t *isolateThread, const std::vector<std::string> &args,
       const dxfg::CommandsContext &context) {
        using namespace std::chrono_literals;

        puts("== LastEventIfSubscribed ==");

        std::size_t argIndex = 0;
        auto address = dxfg::CommandLineParser::parseAddress(args, argIndex, context.getDefaultAddress());

        dxfg_endpoint_t *endpoint = dxfg_DXEndpoint_create(isolateThread);

        dxfg_DXEndpoint_connect(isolateThread, endpoint, address.c_str());

        dxfg_feed_t *feed = dxfg_DXEndpoint_getFeed(isolateThread, endpoint);

        dxfg_candle_symbol_t symbol;
        symbol.supper.type = STRING;
        symbol.symbol = "AAPL";

        dxfg_event_type_t *event = dxfg_DXFeed_getLastEventIfSubscribed(isolateThread, feed, DXFG_EVENT_QUOTE,
                                                                        reinterpret_cast<dxfg_symbol_t *>(&symbol));
        dxfg_subscription_t *subscriptionQuote = dxfg_DXFeed_createSubscription(isolateThread, feed, DXFG_EVENT_QUOTE);

        dxfg_DXFeedSubscription_setSymbol(isolateThread, subscriptionQuote, &symbol.supper);

        event = dxfg_DXFeed_getLastEventIfSubscribed(isolateThread, feed, DXFG_EVENT_QUOTE,
                                                     reinterpret_cast<dxfg_symbol_t *>(&symbol));
        printEvent(isolateThread, event);
        dxfg_EventType_release(isolateThread, event);

        std::this_thread::sleep_for(2s);

        dxfg_DXFeedSubscription_close(isolateThread, subscriptionQuote);
        dxfg_DXEndpoint_close(isolateThread, endpoint);
        dxfg_JavaObjectHandler_release(isolateThread, &subscriptionQuote->handler);
        dxfg_JavaObjectHandler_release(isolateThread, &feed->handler);
        dxfg_JavaObjectHandler_release(isolateThread, &endpoint->handler);
    }};
} // namespace dxfg