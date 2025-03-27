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

void finalize(graal_isolatethread_t * /* thread */, void * /* user_data */);
void observableListModelListenerCallback(graal_isolatethread_t *isolateThread, dxfg_event_type_list *orders,
                                         void * /* user_data */);

namespace dxfg {
inline Command indexedEventModelCase{
    "IndexedEventModelCase",
    {"iem"},
    "",
    "iem [<properties>] [<address>]",
    {"iem %defaultAddress%"},
    [](const Command & /*self*/, graal_isolatethread_t *isolateThread, const std::vector<std::string> &args,
       const dxfg::CommandsContext &context) {
        using namespace std::chrono_literals;

        puts("== IndexedEventModel ==");

        std::size_t argIndex = 0;
        auto address = dxfg::CommandLineParser::parseAddress(args, argIndex, context.getDefaultAddress());

        dxfg_indexed_event_model_t *indexed_event_model =
            dxfg_IndexedEventModel_new(isolateThread, DXFG_EVENT_TIME_AND_SALE);

        dxfg_IndexedEventModel_setSizeLimit(isolateThread, indexed_event_model, 30);

        dxfg_endpoint_t *endpoint = dxfg_DXEndpoint_create(isolateThread);

        dxfg_DXEndpoint_connect(isolateThread, endpoint, address.c_str());

        dxfg_feed_t *feed = dxfg_DXEndpoint_getFeed(isolateThread, endpoint);

        dxfg_IndexedEventModel_attach(isolateThread, indexed_event_model, feed);

        dxfg_observable_list_model_t *observable_list_model =
            dxfg_IndexedEventModel_getEventsList(isolateThread, indexed_event_model);

        dxfg_string_symbol_t symbolAAPL;
        symbolAAPL.supper.type = STRING;
        symbolAAPL.symbol = "AAPL";
        dxfg_IndexedEventModel_setSymbol(isolateThread, indexed_event_model, &symbolAAPL.supper);

        dxfg_observable_list_model_listener_t *observable_list_model_listener =
            dxfg_ObservableListModelListener_new(isolateThread, &observableListModelListenerCallback, nullptr);

        dxfg_Object_finalize(isolateThread,
                             reinterpret_cast<dxfg_java_object_handler *>(observable_list_model_listener), &finalize,
                             nullptr);
        dxfg_ObservableListModel_addListener(isolateThread, observable_list_model, observable_list_model_listener);

        std::this_thread::sleep_for(2s);

        dxfg_IndexedEventModel_close(isolateThread, indexed_event_model);
        dxfg_DXEndpoint_close(isolateThread, endpoint);
        dxfg_JavaObjectHandler_release(isolateThread, &indexed_event_model->handler);
        dxfg_JavaObjectHandler_release(isolateThread, &observable_list_model->handler);
        dxfg_JavaObjectHandler_release(isolateThread, &observable_list_model_listener->handler);
        dxfg_JavaObjectHandler_release(isolateThread, &feed->handler);
        dxfg_JavaObjectHandler_release(isolateThread, &endpoint->handler);
    }};
} // namespace dxfg