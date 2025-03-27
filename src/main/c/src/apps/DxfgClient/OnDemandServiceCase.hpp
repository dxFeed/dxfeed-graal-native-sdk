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

namespace dxfg {
inline Command onDemandServiceCase{
    "OnDemandServiceCase",
    {"ods"},
    "",
    "ods [<properties>] [<onDemandAddress>] [<user> <password>]",
    {"ods %defaultOnDemandAddress%", "ods %defaultOnDemandAddress% %defaultUser% %defaultPassword%"},
    [](const Command & /*self*/, graal_isolatethread_t *isolateThread, const std::vector<std::string> &args,
       const dxfg::CommandsContext &context) {
        using namespace std::chrono_literals;

        puts("== OnDemandService::BEGIN ==");

        std::size_t argIndex = 0;
        auto address = dxfg::CommandLineParser::parseAddress(args, argIndex, context.getDefaultOnDemandAddress());

        dxfg_on_demand_service_t *service = dxfg_OnDemandService_getInstance(isolateThread);
        dxfg_endpoint_t *endpoint = dxfg_OnDemandService_getEndpoint(isolateThread, service);

        auto user = context.getDefaultUser();
        auto password = context.getDefaultPassword();

        if (args.size() == 3) {
            user = CommandLineParser::parseString(args, argIndex, context.getDefaultUser());
            password = CommandLineParser::parseString(args, argIndex, context.getDefaultPassword());
        }

        dxfg_DXEndpoint_user(isolateThread, endpoint, user.c_str());
        dxfg_DXEndpoint_password(isolateThread, endpoint, password.c_str());

        dxfg_DXEndpoint_connect(isolateThread, endpoint, address.c_str());
        dxfg_feed_t *feed = dxfg_DXEndpoint_getFeed(isolateThread, endpoint);
        dxfg_subscription_t *subscriptionQuote = dxfg_DXFeed_createSubscription(isolateThread, feed, DXFG_EVENT_QUOTE);
        dxfg_feed_event_listener_t *listener = dxfg_DXFeedEventListener_new(isolateThread, &printEvents, nullptr);
        dxfg_DXFeedSubscription_addEventListener(isolateThread, subscriptionQuote, listener);
        dxfg_string_symbol_t symbolAAPL;
        symbolAAPL.supper.type = STRING;
        symbolAAPL.symbol = "AAPL";
        dxfg_DXFeedSubscription_setSymbol(isolateThread, subscriptionQuote, &symbolAAPL.supper);
        // 1273171668000 - 2010-05-06 14:47:48.000 EST
        dxfg_OnDemandService_replay(isolateThread, service, 1273171668000);

        std::this_thread::sleep_for(2s);

        dxfg_DXEndpoint_closeAndAwaitTermination(isolateThread, endpoint);
        dxfg_DXFeedSubscription_close(isolateThread, subscriptionQuote);
        dxfg_JavaObjectHandler_release(isolateThread, &subscriptionQuote->handler);
        dxfg_JavaObjectHandler_release(isolateThread, &listener->handler);
        dxfg_JavaObjectHandler_release(isolateThread, &feed->handler);
        dxfg_JavaObjectHandler_release(isolateThread, &endpoint->handler);
        dxfg_JavaObjectHandler_release(isolateThread, &service->handler);
        puts("== OnDemandService::END ==");
    }};
} // namespace dxfg