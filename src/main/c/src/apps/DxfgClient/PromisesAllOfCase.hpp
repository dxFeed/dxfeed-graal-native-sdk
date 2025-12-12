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
#include <vector>

namespace dxfg {
inline Command promisesAllOfCase{
    "PromisesAllOfCase",
    {"pao"},
    "",
    "pao [<properties>] [<address>] [<symbols>]",
    {"pao %defaultAddress% %defaultSymbols%"},
    [](const Command & /*self*/, graal_isolatethread_t *isolateThread, const std::vector<std::string> &args,
       const dxfg::CommandsContext &context) {
        using namespace std::chrono_literals;

        puts("== PromisesAllOf ==");

        std::size_t argIndex = 0;
        auto address = dxfg::CommandLineParser::parseAddress(args, argIndex, context.getDefaultAddress());
        std::vector<std::string> symbols{};

        if (argIndex < args.size()) {
            // Here it is as an example of usage.
            dxfg_string_list *symbolsList = dxfg_Tools_parseSymbols(isolateThread, args[argIndex].c_str());

            if (symbolsList == nullptr) {
                getException(isolateThread);
            } else {
                if (symbolsList->size > 0) {
                    for (auto i = 0; i < symbolsList->size; i++) {
                        if (symbolsList->elements[i] == nullptr) {
                            continue;
                        }

                        symbols.emplace_back(symbolsList->elements[i]);
                    }
                }

                dxfg_CList_String_release(isolateThread, symbolsList);
            }
        }

        if (symbols.empty()) {
            symbols = context.getDefaultSymbols();
        }

        dxfg_endpoint_t *endpoint = dxfg_DXEndpoint_create(isolateThread);

        dxfg_DXEndpoint_connect(isolateThread, endpoint, address.c_str());

        dxfg_feed_t *feed = dxfg_DXEndpoint_getFeed(isolateThread, endpoint);
        size_t size = symbols.size();
        dxfg_symbol_list symbolList;

        symbolList.size = (int32_t)size;
        symbolList.elements = new dxfg_symbol_t *[size] {
        };

        for (int i = 0; i < size; i++) {
            auto symbol = new dxfg_string_symbol_t{};

            symbol->supper.type = STRING;
            symbol->symbol = symbols[i].c_str();
            symbolList.elements[i] = &symbol->supper;
        }

        dxfg_promise_list *promises =
            dxfg_DXFeed_getLastEventsPromises(isolateThread, feed, DXFG_EVENT_QUOTE, &symbolList);
        dxfg_promise_t *all = dxfg_Promises_allOf(isolateThread, promises);

        dxfg_Promise_awaitWithoutException(isolateThread, all, 30000);

        if (dxfg_Promise_hasResult(isolateThread, all) == 1) {
            for (int i = 0; i < size; ++i) {
                dxfg_event_type_t *event = dxfg_Promise_EventType_getResult(
                    isolateThread, reinterpret_cast<dxfg_promise_event_t *>(promises->list.elements[i]));

                printEvent(isolateThread, event);
                dxfg_EventType_release(isolateThread, event);
            }
        }

        dxfg_DXEndpoint_close(isolateThread, endpoint);

        for (int i = 0; i < size; i++) {
            delete symbolList.elements[i];
        }

        delete symbolList.elements;

        dxfg_JavaObjectHandler_release(isolateThread, &all->handler);
        dxfg_CList_JavaObjectHandler_release(isolateThread, &promises->list);
        dxfg_JavaObjectHandler_release(isolateThread, &feed->handler);
        dxfg_JavaObjectHandler_release(isolateThread, &endpoint->handler);
    }};
} // namespace dxfg
