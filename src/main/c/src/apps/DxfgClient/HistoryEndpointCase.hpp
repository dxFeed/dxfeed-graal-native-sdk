// Copyright (c) 2025 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

#pragma once

#include <dxfg_api.h>

#include "CommandLineParser.hpp"
#include "CommandsContext.hpp"
#include "CommandsRegistry.hpp"
#include "Common.hpp"

#include <chrono>
#include <cstdio>
#include <string>
#include <thread>
#include <vector>

void getException(graal_isolatethread_t *isolateThread);
void printEvents(graal_isolatethread_t *isolateThread, dxfg_event_type_list *events, void * /* user_data */);

namespace dxfg {
inline Command historyEndpointCase{
    "HistoryEndpointCase",
    {"he"},
    "",
    "he [<properties>] <address> <user> <password> <fromDateTime> <toDateTime>",
    {"he %defaultCandleDataAddress% demo demo 20250325-090000 20250327-090000"},
    [](const Command & /*self*/, graal_isolatethread_t *isolateThread, const std::vector<std::string> &args,
       const dxfg::CommandsContext &) {
        using namespace std::chrono_literals;

        puts("== HistoryEndpoint ==");

        std::size_t argIndex = 0;
        const auto address = dxfg::CommandLineParser::parseAddress(args, argIndex, "UNKNOWN");
        const auto user = dxfg::CommandLineParser::parseString(args, argIndex, "UNKNOWN");
        const auto password = dxfg::CommandLineParser::parseString(args, argIndex, "UNKNOWN");
        const auto fromDateTime = dxfg::CommandLineParser::parseString(args, argIndex, "UNKNOWN");
        const auto toDateTime = dxfg::CommandLineParser::parseString(args, argIndex, "UNKNOWN");

        if (address == "UNKNOWN" || user == "UNKNOWN" || password == "UNKNOWN" || fromDateTime == "UNKNOWN" ||
            toDateTime == "UNKNOWN") {
            puts("Parameters not specified");

            return;
        }

        dxfg_history_endpoint_builder_t *builder{};
        auto result = dxfg_HistoryEndpoint_newBuilder(isolateThread, &builder);

        if (result != DXFG_EXECUTE_SUCCESSFULLY) {
            getException(isolateThread);

            return;
        }
        result = dxfg_HistoryEndpoint_Builder_withAddress(isolateThread, builder, address.c_str());

        finally([&] {
            dxfg_JavaObjectHandler_release(isolateThread, &builder->handler);
        });

        if (result != DXFG_EXECUTE_SUCCESSFULLY) {
            getException(isolateThread);

            return;
        }

        result = dxfg_HistoryEndpoint_Builder_withUserName(isolateThread, builder, user.c_str());

        if (result != DXFG_EXECUTE_SUCCESSFULLY) {
            getException(isolateThread);

            return;
        }

        result = dxfg_HistoryEndpoint_Builder_withPassword(isolateThread, builder, password.c_str());

        if (result != DXFG_EXECUTE_SUCCESSFULLY) {
            getException(isolateThread);

            return;
        }

        dxfg_history_endpoint_t *endpoint{};

        result = dxfg_HistoryEndpoint_Builder_build(isolateThread, builder, &endpoint);

        if (result != DXFG_EXECUTE_SUCCESSFULLY) {
            getException(isolateThread);

            return;
        }

        finally([&] {
            dxfg_JavaObjectHandler_release(isolateThread, &endpoint->handler);
        });

        dxfg_time_format_t *format = dxfg_TimeFormat_DEFAULT(isolateThread);

        if (format == nullptr) {
            getException(isolateThread);

            return;
        }

        finally([&] {
            dxfg_JavaObjectHandler_release(isolateThread, &format->handler);
        });

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

        dxfg_string_symbol_t symbolAAPLd{};

        symbolAAPLd.supper.type = STRING;
        symbolAAPLd.symbol = "AAPL{=d}";

        dxfg_event_type_list *events{};

        result = dxfg_HistoryEndpoint_getTimeSeries(isolateThread, endpoint, DXFG_EVENT_CANDLE,
                                                    (dxfg_symbol_t *)&symbolAAPLd, from, to, &events);

        if (result != DXFG_EXECUTE_SUCCESSFULLY) {
            getException(isolateThread);
        }

        finally([&] {
            dxfg_CList_EventType_release(isolateThread, events);
        });

        printEvents(isolateThread, events, nullptr);
    }};
} // namespace dxfg
