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

void printEvents(graal_isolatethread_t *isolateThread, dxfg_event_type_list *events, void * /* user_data */);
void finalize(graal_isolatethread_t * /* thread */, void * /* user_data */);

namespace dxfg {
inline Command dxEndpointSubscriptionCase{
    "DxEndpointSubscriptionCase",
    {"es"},
    "",
    "es [<properties>] [<address>]",
    {"es %defaultAddress%"},
    [](const Command & /*self*/, graal_isolatethread_t *isolateThread, const std::vector<std::string> &args,
       const dxfg::CommandsContext &context) {
        using namespace std::chrono_literals;

        puts("== DxEndpointSubscription ==");

        std::size_t argIndex = 0;
        auto address = dxfg::CommandLineParser::parseAddress(args, argIndex, context.getDefaultAddress());

        dxfg_endpoint_t *endpoint = dxfg_DXEndpoint_create(isolateThread);

        dxfg_DXEndpoint_connect(isolateThread, endpoint, address.c_str());

        dxfg_feed_t *feed = dxfg_DXEndpoint_getFeed(isolateThread, endpoint);
        dxfg_subscription_t *subscriptionQuote = dxfg_DXFeed_createSubscription(isolateThread, feed, DXFG_EVENT_QUOTE);
        dxfg_feed_event_listener_t *listener = dxfg_DXFeedEventListener_new(isolateThread, &printEvents, nullptr);

        dxfg_Object_finalize(isolateThread, reinterpret_cast<dxfg_java_object_handler *>(listener), finalize, nullptr);
        dxfg_DXFeedSubscription_addEventListener(isolateThread, subscriptionQuote, listener);

        dxfg_string_symbol_t symbolAAPL;
        symbolAAPL.supper.type = STRING;
        symbolAAPL.symbol = "AAPL";

        dxfg_DXFeedSubscription_setSymbol(isolateThread, subscriptionQuote, &symbolAAPL.supper);

        const int containsQuote =
            dxfg_DXFeedSubscription_containsEventType(isolateThread, subscriptionQuote, DXFG_EVENT_QUOTE);

        printf("  contains Quote: %d\n", containsQuote);

        const int containsCandle =
            dxfg_DXFeedSubscription_containsEventType(isolateThread, subscriptionQuote, DXFG_EVENT_CANDLE);

        printf("  contains Candle: %d\n", containsCandle);

        std::this_thread::sleep_for(2s);

        dxfg_DXFeedSubscription_close(isolateThread, subscriptionQuote);
        dxfg_DXEndpoint_close(isolateThread, endpoint);
        dxfg_JavaObjectHandler_release(isolateThread, &subscriptionQuote->handler);
        dxfg_JavaObjectHandler_release(isolateThread, &listener->handler);
        dxfg_JavaObjectHandler_release(isolateThread, &feed->handler);
        dxfg_JavaObjectHandler_release(isolateThread, &endpoint->handler);
    }};
} // namespace dxfg
