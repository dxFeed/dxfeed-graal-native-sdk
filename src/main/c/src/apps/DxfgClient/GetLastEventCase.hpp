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
inline Command getLastEventCase{
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

        // dxfg_DXFeed_getLastEvent2 and dxfg_DXFeed_getLastEvents2 only read the given events, so they are the caller's
        // own structures here. The results are new events.
        dxfg_candle_t aapl{};
        aapl.event_type.clazz = DXFG_EVENT_CANDLE;
        aapl.event_symbol = "AAPL";

        dxfg_candle_t ibm{};
        ibm.event_type.clazz = DXFG_EVENT_CANDLE;
        ibm.event_symbol = "IBM";

        auto printLastEvents = [isolateThread, feed, &aapl, &ibm] {
            dxfg_event_type_t *lastEvent = nullptr;

            if (dxfg_DXFeed_getLastEvent2(isolateThread, feed, &aapl.event_type, &lastEvent) ==
                DXFG_EXECUTE_SUCCESSFULLY) {
                printEvent(isolateThread, lastEvent);
                dxfg_EventType_release(isolateThread, lastEvent);
            }

            dxfg_event_type_t *events[] = {&aapl.event_type, &ibm.event_type};
            dxfg_event_type_list eventList{2, events};
            dxfg_event_type_list *lastEvents = nullptr;

            if (dxfg_DXFeed_getLastEvents2(isolateThread, feed, &eventList, &lastEvents) == DXFG_EXECUTE_SUCCESSFULLY) {
                for (int32_t i = 0; i < lastEvents->size; ++i) {
                    printEvent(isolateThread, lastEvents->elements[i]);
                }

                dxfg_CList_EventType_release(isolateThread, lastEvents);
            }
        };

        printLastEvents();
        std::this_thread::sleep_for(2s);
        printLastEvents();
        std::this_thread::sleep_for(2s);
        printLastEvents();

        dxfg_DXFeedSubscription_close(isolateThread, subscription);
        dxfg_DXEndpoint_close(isolateThread, endpoint);
        dxfg_JavaObjectHandler_release(isolateThread, &subscription->handler);
        dxfg_JavaObjectHandler_release(isolateThread, &feed->handler);
        dxfg_JavaObjectHandler_release(isolateThread, &endpoint->handler);
    }};
} // namespace dxfg