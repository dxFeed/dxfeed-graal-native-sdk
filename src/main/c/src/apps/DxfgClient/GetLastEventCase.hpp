// Copyright (c) 2024 Devexperts LLC.
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
Command getLastEventCase{
    "GetLastEventCase",
    {"gle"},
    "",
    "gle [<properties>] [<address>]",
    {"gle %defaultAddress%"},
    [](const Command & /*self*/, graal_isolatethread_t *isolateThread, const std::vector<std::string> &args,
       const dxfg::CommandsContext &context) {
        using namespace std::chrono_literals;

        puts("== GetLastEvent ==");

        std::size_t argIndex = 0;
        auto address = dxfg::CommandLineParser::parseAddress(args, argIndex, context.getDefaultAddress());
        dxfg_endpoint_t *endpoint = dxfg_DXEndpoint_create(isolateThread);

        dxfg_DXEndpoint_connect(isolateThread, endpoint, address.c_str());

        dxfg_feed_t *feed = dxfg_DXEndpoint_getFeed(isolateThread, endpoint);

        dxfg_candle_symbol_t symbol;
        symbol.supper.type = CANDLE;
        symbol.symbol = "AAPL";

        dxfg_subscription_t *subscription = dxfg_DXFeed_createSubscription(isolateThread, feed, DXFG_EVENT_CANDLE);

        dxfg_DXFeedSubscription_setSymbol(isolateThread, subscription, &symbol.supper);

        auto *candle = (dxfg_candle_t *)dxfg_EventType_new(isolateThread, symbol.symbol, DXFG_EVENT_CANDLE);

        dxfg_DXFeed_getLastEvent(isolateThread, feed, &candle->event_type);
        printEvent(isolateThread, &candle->event_type);

        dxfg_event_type_list event_type_list{};
        event_type_list.size = 2;
        event_type_list.elements = new dxfg_event_type_t *[2]{};
        event_type_list.elements[0] = dxfg_EventType_new(isolateThread, "AAPL", DXFG_EVENT_CANDLE);
        event_type_list.elements[1] = dxfg_EventType_new(isolateThread, "IBM", DXFG_EVENT_CANDLE);

        printEvent(isolateThread, event_type_list.elements[0]);
        printEvent(isolateThread, event_type_list.elements[1]);

        std::this_thread::sleep_for(2s);

        dxfg_DXFeed_getLastEvent(isolateThread, feed, &candle->event_type);
        printEvent(isolateThread, &candle->event_type);
        dxfg_DXFeed_getLastEvents(isolateThread, feed, &event_type_list);
        printEvent(isolateThread, event_type_list.elements[0]);
        printEvent(isolateThread, event_type_list.elements[1]);

        std::this_thread::sleep_for(2s);

        dxfg_DXFeed_getLastEvent(isolateThread, feed, &candle->event_type);
        printEvent(isolateThread, &candle->event_type);
        dxfg_DXFeed_getLastEvents(isolateThread, feed, &event_type_list);
        printEvent(isolateThread, event_type_list.elements[0]);
        printEvent(isolateThread, event_type_list.elements[1]);

        delete[] event_type_list.elements;

        dxfg_DXFeedSubscription_close(isolateThread, subscription);
        dxfg_DXEndpoint_close(isolateThread, endpoint);
        dxfg_EventType_release(isolateThread, &candle->event_type);
        dxfg_JavaObjectHandler_release(isolateThread, &subscription->handler);
        dxfg_JavaObjectHandler_release(isolateThread, &feed->handler);
        dxfg_JavaObjectHandler_release(isolateThread, &endpoint->handler);
    }};
} // namespace dxfg