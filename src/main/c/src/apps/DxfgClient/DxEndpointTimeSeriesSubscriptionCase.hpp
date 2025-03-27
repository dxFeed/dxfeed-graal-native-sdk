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
inline Command dxEndpointTimeSeriesSubscriptionCase{
    "DxEndpointTimeSeriesSubscriptionCase",
    {"ets"},
    "",
    "ets [<properties>] [<address>]",
    {"ets %defaultAddress%"},
    [](const Command & /*self*/, graal_isolatethread_t *isolateThread, const std::vector<std::string> &args,
       const dxfg::CommandsContext &context) {
        using namespace std::chrono_literals;

        puts("== DxEndpointTimeSeriesSubscription ==");

        std::size_t argIndex = 0;
        auto address = dxfg::CommandLineParser::parseAddress(args, argIndex, context.getDefaultAddress());

        dxfg_endpoint_t *endpoint = dxfg_DXEndpoint_create(isolateThread);

        dxfg_DXEndpoint_connect(isolateThread, endpoint, address.c_str());

        dxfg_feed_t *feed = dxfg_DXEndpoint_getFeed(isolateThread, endpoint);

        dxfg_event_clazz_list_t *event_clazz_list = dxfg_DXEndpoint_getEventTypes(isolateThread, endpoint);
        dxfg_time_series_subscription_t *subscriptionTaS =
            dxfg_DXFeed_createTimeSeriesSubscription2(isolateThread, feed, event_clazz_list);
        dxfg_CList_EventClazz_release(isolateThread, event_clazz_list);

        dxfg_DXFeedTimeSeriesSubscription_setFromTime(isolateThread, subscriptionTaS, 0);

        dxfg_feed_event_listener_t *listener = dxfg_DXFeedEventListener_new(isolateThread, &printEvents, nullptr);

        dxfg_Object_finalize(isolateThread, reinterpret_cast<dxfg_java_object_handler *>(listener), finalize, nullptr);
        dxfg_DXFeedSubscription_addEventListener(isolateThread, &subscriptionTaS->sub, listener);

        dxfg_string_symbol_t symbolAAPL;
        symbolAAPL.supper.type = STRING;
        symbolAAPL.symbol = "AAPL";
        dxfg_DXFeedSubscription_setSymbol(isolateThread, &subscriptionTaS->sub, &symbolAAPL.supper);

        std::this_thread::sleep_for(10s);

        dxfg_DXFeedSubscription_close(isolateThread, reinterpret_cast<dxfg_subscription_t *>(subscriptionTaS));
        dxfg_DXEndpoint_close(isolateThread, endpoint);
        dxfg_JavaObjectHandler_release(isolateThread, &subscriptionTaS->sub.handler);
        dxfg_JavaObjectHandler_release(isolateThread, &listener->handler);
        dxfg_JavaObjectHandler_release(isolateThread, &feed->handler);
        dxfg_JavaObjectHandler_release(isolateThread, &endpoint->handler);
    }};
} // namespace dxfg