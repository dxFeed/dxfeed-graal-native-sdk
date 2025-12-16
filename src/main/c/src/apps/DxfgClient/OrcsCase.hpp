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

inline std::optional<int64_t> parseGranularityAsMillis(graal_isolatethread_t *isolateThread,
                                               const std::string &candleSymbolString) {
    if (const auto granularityKeyValueIndex = candleSymbolString.find("gr=");
        granularityKeyValueIndex != std::string::npos) {
        if (const auto closingCurlyBraceIndex = candleSymbolString.find('}', granularityKeyValueIndex);
            closingCurlyBraceIndex != std::string::npos) {
            const auto granularityValueIndex = granularityKeyValueIndex + 3;
            const auto granularityValueString =
                candleSymbolString.substr(granularityValueIndex, closingCurlyBraceIndex - granularityValueIndex);
            const auto timePeriod = dxfg_TimePeriod_valueOf2(isolateThread, granularityValueString.c_str());

            finally([&] {
                dxfg_JavaObjectHandler_release(isolateThread, &timePeriod->handler);
            });

            if (timePeriod) {
                auto result = dxfg_TimePeriod_getTime(isolateThread, timePeriod);

                if (result == std::numeric_limits<int64_t>::min()) {
                    return std::nullopt;
                }

                return result;
            }
        }
    }

    return std::nullopt;
}

inline Command orcsCase{
    "OrcsCase",
    {"orcs"},
    "",
    R"(orcs [<properties>] <address> <candleSymbol> <orderSource> <fromDateTime> <toDateTime>)",
    {R"(orcs localhost:6666[login=entitle:<TOKEN>] "/6B:XCME{=d,gr=10m}" "GLBX" "20251208-000000-0500" "20251208-235959-0500")"},
    [](const Command & /*self*/, graal_isolatethread_t *isolateThread, const std::vector<std::string> &args,
       const dxfg::CommandsContext &) {
        using namespace std::chrono_literals;

        std::size_t argIndex = 0;
        auto address = dxfg::CommandLineParser::parseAddress(args, argIndex, "UNKNOWN");
        auto candleSymbolString = dxfg::CommandLineParser::parseString(args, argIndex, "UNKNOWN");
        auto orderSourceString = dxfg::CommandLineParser::parseString(args, argIndex, "UNKNOWN");
        auto fromDateTime = dxfg::CommandLineParser::parseString(args, argIndex, "UNKNOWN");
        auto toDateTime = dxfg::CommandLineParser::parseString(args, argIndex, "UNKNOWN");

        puts("== OrcsCase ==");

        if (address == "UNKNOWN" || candleSymbolString == "UNKNOWN" || orderSourceString == "UNKNOWN" ||
            fromDateTime == "UNKNOWN" || toDateTime == "UNKNOWN") {
            puts("Parameters not specified");

            return;
        }

        dxfg_price_level_service_t *service{};

        auto result = dxfg_PriceLevelService_new(isolateThread, address.c_str(), &service);

        if (result != DXFG_EXECUTE_SUCCESSFULLY) {
            getException(isolateThread);

            return;
        }

        finally([&] {
            dxfg_PriceLevelService_close(isolateThread, service);
            dxfg_JavaObjectHandler_release(isolateThread, &service->handler);
        });

        dxfg_auth_order_source_t *authOrderSource{};

        result = dxfg_PriceLevelService_getAuthOrderSource(isolateThread, service, &authOrderSource);

        if (result != DXFG_EXECUTE_SUCCESSFULLY) {
            getException(isolateThread);

            return;
        }

        finally([&] {
            dxfg_JavaObjectHandler_release(isolateThread, &authOrderSource->handler);
        });

        dxfg_symbols_by_order_source_map_entry_list_t *symbolsByOrder{};

        result = dxfg_AuthOrderSource_getByOrderSources(isolateThread, authOrderSource, &symbolsByOrder);

        if (result != DXFG_EXECUTE_SUCCESSFULLY) {
            getException(isolateThread);

            return;
        }

        finally([&] {
            dxfg_symbols_by_order_source_map_entry_list_free(isolateThread, symbolsByOrder);
        });

        printf("Authorized sources: [");

        for (int32_t i = 0; i < symbolsByOrder->size; ++i) {
            printf("%s%s", symbolsByOrder->elements[i]->order_source->name, i == symbolsByOrder->size - 1 ? "" : ", ");
        }

        printf("]\n");

        for (int32_t i = 0; i < symbolsByOrder->size; ++i) {
            printf("%s: [", symbolsByOrder->elements[i]->order_source->name);

            for (int32_t j = 0; j < symbolsByOrder->elements[i]->symbols->size; ++j) {
                printf("%s%s", symbolsByOrder->elements[i]->symbols->elements[j],
                       j == symbolsByOrder->elements[i]->symbols->size - 1 ? "" : ", ");
            }

            printf("]\n\n");
        }

        dxfg_time_format_t *format = dxfg_TimeFormat_GMT(isolateThread);

        if (format == nullptr) {
            getException(isolateThread);

            return;
        }

        finally([&] {
            dxfg_JavaObjectHandler_release(isolateThread, &format->handler);
        });

        dxfg_candle_symbol_t candleSymbol{.supper = {.type = CANDLE}, .symbol = candleSymbolString.c_str()};

        dxfg_indexed_event_source_t *orderSource =
            dxfg_IndexedEventSource_new(isolateThread, orderSourceString.c_str());

        if (!orderSource) {
            getException(isolateThread);

            return;
        }

        const int64_t from = dxfg_TimeFormat_parse(isolateThread, format, fromDateTime.c_str());

        if (from == -1) {
            getException(isolateThread);

            return;
        }

        const int64_t to = dxfg_TimeFormat_parse(isolateThread, format, toDateTime.c_str());

        if (to == -1) {
            getException(isolateThread);

            return;
        }

        StopWatch stopWatch{};
        stopWatch.start();

        dxfg_event_type_list *orders{};

        result = dxfg_PriceLevelService_getOrders2(isolateThread, service, &candleSymbol.supper, orderSource, from, to,
                                                   &orders);

        if (result != DXFG_EXECUTE_SUCCESSFULLY) {
            getException(isolateThread);

            return;
        }

        finally([&] {
            dxfg_CList_EventType_release(isolateThread, orders);
        });

        stopWatch.stop();

        printf("Received %d orders in %lldms\n", orders->size, stopWatch.elapsed().count());

        auto timeGapBound = parseGranularityAsMillis(isolateThread, candleSymbolString).value_or(60000);

        int32_t isValid = 0;

        result = dxfg_PriceLevelChecker_validate(isolateThread, orders, timeGapBound, 0, &isValid);

        if (result != DXFG_EXECUTE_SUCCESSFULLY) {
            getException(isolateThread);

            return;
        }

        printf("Orders are valid: %s\n", isValid ? "true" : "false");

        stopWatch.start();

        dxfg_event_type_list *quotes{};

        result = dxfg_PriceLevelService_getQuotes2(isolateThread, service, &candleSymbol.supper, from, to, &quotes);

        if (result != DXFG_EXECUTE_SUCCESSFULLY) {
            getException(isolateThread);

            return;
        }

        finally([&] {
            dxfg_CList_EventType_release(isolateThread, quotes);
        });

        stopWatch.stop();

        printf("Received %d quotes in %lldms\n", quotes->size, stopWatch.elapsed().count());
    }};
} // namespace dxfg