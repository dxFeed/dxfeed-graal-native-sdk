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

inline void orderBookModelListenerCallback(graal_isolatethread_t *isolateThread, dxfg_order_book_model_t *model,
                                           void * /* user_data */) {
    puts("  Buy orders:");

    dxfg_observable_list_model_t *buyObservableListOrders = dxfg_OrderBookModel_getBuyOrders(isolateThread, model);
    dxfg_event_type_list *buyOrders = dxfg_ObservableListModel_toArray(isolateThread, buyObservableListOrders);

    for (int i = 0; i < buyOrders->size; ++i) {
        printEvent(isolateThread, reinterpret_cast<dxfg_event_type_t *>(buyOrders->elements[0]));
    }

    dxfg_CList_EventType_release(isolateThread, buyOrders);
    dxfg_JavaObjectHandler_release(isolateThread, &buyObservableListOrders->handler);

    puts("  Sell orders:");

    dxfg_observable_list_model_t *sellObservableListOrders = dxfg_OrderBookModel_getSellOrders(isolateThread, model);
    dxfg_event_type_list *sellOrders = dxfg_ObservableListModel_toArray(isolateThread, sellObservableListOrders);

    for (int i = 0; i < sellOrders->size; ++i) {
        printEvent(isolateThread, reinterpret_cast<dxfg_event_type_t *>(sellOrders->elements[0]));
    }

    dxfg_CList_EventType_release(isolateThread, sellOrders);
    dxfg_JavaObjectHandler_release(isolateThread, &sellObservableListOrders->handler);

    printf("\n");
}

inline Command orderBookModelCase{
    "OrderBookModelCase",
    {"obm"},
    "",
    "obm [<properties>] [<address>]",
    {"obm %defaultAddress%"},
    [](const Command & /*self*/, graal_isolatethread_t *isolateThread, const std::vector<std::string> &args,
       const dxfg::CommandsContext &context) {
        using namespace std::chrono_literals;

        puts("== OrderBookModel ==");

        std::size_t argIndex = 0;
        auto address = dxfg::CommandLineParser::parseAddress(args, argIndex, context.getDefaultAddress());

        dxfg_order_book_model_t *order_book_model = dxfg_OrderBookModel_new(isolateThread);

        dxfg_OrderBookModel_setFilter(isolateThread, order_book_model, ALL);
        dxfg_OrderBookModel_setSymbol(isolateThread, order_book_model, "AAPL");

        dxfg_observable_list_model_t *buyOrders = dxfg_OrderBookModel_getBuyOrders(isolateThread, order_book_model);
        dxfg_observable_list_model_listener_t *buyOrdersListener =
            dxfg_ObservableListModelListener_new(isolateThread, &observableListModelListenerCallback, nullptr);

        dxfg_Object_finalize(isolateThread, reinterpret_cast<dxfg_java_object_handler *>(buyOrdersListener), &finalize,
                             nullptr);
        dxfg_ObservableListModel_addListener(isolateThread, buyOrders, buyOrdersListener);

        dxfg_observable_list_model_t *sellOrders = dxfg_OrderBookModel_getSellOrders(isolateThread, order_book_model);
        dxfg_observable_list_model_listener_t *sellOrdersListener =
            dxfg_ObservableListModelListener_new(isolateThread, &observableListModelListenerCallback, nullptr);

        dxfg_Object_finalize(isolateThread, reinterpret_cast<dxfg_java_object_handler *>(sellOrdersListener), &finalize,
                             nullptr);
        dxfg_ObservableListModel_addListener(isolateThread, sellOrders, sellOrdersListener);

        dxfg_order_book_model_listener_t *listener =
            dxfg_OrderBookModelListener_new(isolateThread, &orderBookModelListenerCallback, nullptr);

        dxfg_Object_finalize(isolateThread, reinterpret_cast<dxfg_java_object_handler *>(listener), &finalize, nullptr);
        dxfg_OrderBookModel_addListener(isolateThread, order_book_model, listener);
        dxfg_endpoint_t *endpoint = dxfg_DXEndpoint_create(isolateThread);
        dxfg_DXEndpoint_connect(isolateThread, endpoint, address.c_str());
        dxfg_feed_t *feed = dxfg_DXEndpoint_getFeed(isolateThread, endpoint);
        dxfg_OrderBookModel_attach(isolateThread, order_book_model, feed);

        std::this_thread::sleep_for(2s);

        dxfg_OrderBookModel_close(isolateThread, order_book_model);
        dxfg_DXEndpoint_close(isolateThread, endpoint);
        dxfg_JavaObjectHandler_release(isolateThread, &order_book_model->handler);
        dxfg_JavaObjectHandler_release(isolateThread, &buyOrders->handler);
        dxfg_JavaObjectHandler_release(isolateThread, &buyOrdersListener->handler);
        dxfg_JavaObjectHandler_release(isolateThread, &sellOrders->handler);
        dxfg_JavaObjectHandler_release(isolateThread, &sellOrdersListener->handler);
        dxfg_JavaObjectHandler_release(isolateThread, &listener->handler);
        dxfg_JavaObjectHandler_release(isolateThread, &feed->handler);
        dxfg_JavaObjectHandler_release(isolateThread, &endpoint->handler);
    }};
} // namespace dxfg