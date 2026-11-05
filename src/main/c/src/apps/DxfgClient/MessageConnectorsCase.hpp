// Copyright (c) 2026 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

#pragma once

#include "Common.hpp"

#include <dxfg_api.h>

#include "CommandLineParser.hpp"
#include "CommandsContext.hpp"
#include "CommandsRegistry.hpp"

#include <chrono>
#include <cinttypes>
#include <cstdio>
#include <map>
#include <string>
#include <thread>
#include <vector>

namespace dxfg {
namespace message_connectors {

inline const char *kindName(int32_t kind) {
    switch (kind) {
    case DXFG_V4_MESSAGE_CONNECTOR_KIND_CLIENT_SOCKET:
        return "client socket";
    case DXFG_V4_MESSAGE_CONNECTOR_KIND_SERVER_SOCKET:
        return "server socket";
    default:
        return "other";
    }
}

inline const char *stateName(int32_t state) {
    switch (state) {
    case DXFG_V4_MESSAGE_CONNECTOR_STATE_CONNECTING:
        return "CONNECTING";
    case DXFG_V4_MESSAGE_CONNECTOR_STATE_CONNECTED:
        return "CONNECTED";
    case DXFG_V4_MESSAGE_CONNECTOR_STATE_DISCONNECTED:
        return "DISCONNECTED";
    default:
        return "?";
    }
}

// Takes a string the SDK allocated and releases it.
inline std::string takeString(graal_isolatethread_t *thread, const char *value) {
    std::string result = value != nullptr ? value : "";
    dxfg_v4_String_release(thread, value);
    return result;
}

inline std::string addressOf(graal_isolatethread_t *thread, dxfg_v4_message_connector_t *connector) {
    const char *address = nullptr;

    if (dxfg_v4_MessageConnector_getAddress(thread, connector, &address) != DXFG_EXECUTE_SUCCESSFULLY) {
        return "?";
    }

    return takeString(thread, address);
}

inline std::string currentAddressesOf(graal_isolatethread_t *thread, dxfg_v4_message_connector_t *connector) {
    dxfg_v4_string_list *addresses = nullptr;

    if (dxfg_v4_MessageConnector_getCurrentAddresses(thread, connector, &addresses) != DXFG_EXECUTE_SUCCESSFULLY) {
        return "?";
    }

    std::string result;

    for (int32_t i = 0; i < addresses->size; i++) {
        result += (i == 0 ? "" : ", ") + std::string(addresses->elements[i]);
    }

    dxfg_v4_string_list_release(thread, addresses);

    return result.empty() ? "-" : result;
}

// Called on the thread of the SDK `dxfg-MessageConnectorListener` after a state change. The handle is valid during
// the call only; the function reads the current state, which may be newer than the change that caused the call.
inline void onStateChanged(graal_isolatethread_t *thread, dxfg_v4_message_connector_t *connector, void * /*userData*/) {
    int32_t state = -1;
    dxfg_v4_MessageConnector_getState(thread, connector, &state);
    printf("  listener: %s is %s\n", addressOf(thread, connector).c_str(), stateName(state));
}

} // namespace message_connectors

inline Command messageConnectorsCase{
    "MessageConnectorsCase",
    {"mc"},
    "Prints the connectors of a feed endpoint every second (state, current addresses, statistics) and their state "
    "changes from a connector listener",
    "mc [<properties>] [<address>] [<symbol>] [<seconds>]",
    {"mc %defaultAddress% AAPL 10", "mc 127.0.0.1:1 AAPL 5"},
    [](const Command & /*self*/, graal_isolatethread_t *isolateThread, const std::vector<std::string> &args,
       const dxfg::CommandsContext &context) {
        using namespace dxfg::message_connectors;

        puts("== MessageConnectors::BEGIN ==");

        std::size_t argIndex = 0;
        const auto address = dxfg::CommandLineParser::parseAddress(args, argIndex, context.getDefaultAddress());
        const auto symbol = dxfg::CommandLineParser::parseString(args, argIndex, "AAPL");
        const auto seconds = std::stoi(dxfg::CommandLineParser::parseString(args, argIndex, "10"));

        int32_t supported = 0;
        dxfg_v4_system_is_feature_supported(isolateThread, DXFG_V4_FEATURE_MESSAGE_CONNECTORS, &supported);

        if (supported != 1) {
            puts("The library has no message connectors");
            puts("== MessageConnectors::END ==");

            return;
        }

        // The listener is experimental: use it only when the library has the revision these headers are built for.
        int32_t revision = -1;
        dxfg_v4_system_get_advanced_revision(isolateThread, &revision);
        const bool useListener = revision == DXFG_V4_ADVANCED_REVISION;

        dxfg_v4_endpoint_t *endpoint = nullptr;
        dxfg_v4_DXEndpoint_createWithRole(isolateThread, DXFG_ENDPOINT_ROLE_FEED, &endpoint);

        dxfg_v4_message_connector_listener_t *listener = nullptr;

        if (useListener) {
            dxfg_v4_MessageConnectorListener_new(isolateThread, &onStateChanged, nullptr, &listener);
            // Before the connect: a listener gets the later changes only, not the current states.
            dxfg_v4_DXEndpoint_addMessageConnectorListener(isolateThread, endpoint, listener);
        }

        if (dxfg_v4_DXEndpoint_connect(isolateThread, endpoint, address.c_str()) != DXFG_EXECUTE_SUCCESSFULLY) {
            // E.g. a malformed address: the endpoint has no connectors.
            printf("The connect to %s failed\n", address.c_str());
            getException(isolateThread);
        }

        // A subscription makes the data flow, so that the data records read grow.
        dxfg_v4_feed_t *feed = nullptr;
        dxfg_v4_subscription_t *subscription = nullptr;
        dxfg_v4_string_symbol_t quoteSymbol{{STRING}, symbol.c_str()};
        dxfg_v4_DXEndpoint_getFeed(isolateThread, endpoint, &feed);
        dxfg_v4_DXFeed_createSubscription(isolateThread, feed, DXFG_EVENT_QUOTE, &subscription);
        dxfg_v4_DXFeedSubscription_addSymbol(isolateThread, subscription, &quoteSymbol.supper);

        // The previous statistics of each connector, by its configured address, for the rates.
        std::map<std::string, dxfg_v4_message_connector_stats_t> previous;

        for (int second = 1; second <= seconds; second++) {
            std::this_thread::sleep_for(std::chrono::seconds(1));

            // The list is read anew every time: a connect to another address replaces the connectors.
            dxfg_v4_message_connector_list *connectors = nullptr;

            if (dxfg_v4_DXEndpoint_getConnectors(isolateThread, endpoint, &connectors) != DXFG_EXECUTE_SUCCESSFULLY) {
                break;
            }

            for (int32_t i = 0; i < connectors->size; i++) {
                dxfg_v4_message_connector_t *connector = connectors->elements[i];
                int32_t kind = -1;
                int32_t state = -1;
                dxfg_v4_message_connector_stats_t stats{};

                dxfg_v4_MessageConnector_getKind(isolateThread, connector, &kind);
                dxfg_v4_MessageConnector_getState(isolateThread, connector, &state);
                dxfg_v4_MessageConnector_getStatistics(isolateThread, connector, &stats);

                const auto configured = addressOf(isolateThread, connector);
                const auto last = previous.find(configured);
                const auto before = last != previous.end() ? last->second : stats;

                // Data flows when the data records read grow; the bytes read grow on heartbeats too.
                printf("%3ds %s (%s) %s, connected to %s, connections %" PRId64 " open %" PRId64 " closed, "
                       "read %" PRId64 " B (%" PRId64 " B/s), %" PRId64 " records (%" PRId64 " records/s)\n",
                       second, configured.c_str(), kindName(kind), stateName(state),
                       currentAddressesOf(isolateThread, connector).c_str(), stats.active_connection_count,
                       stats.closed_connection_count, stats.read_bytes, stats.read_bytes - before.read_bytes,
                       stats.read_data_records, stats.read_data_records - before.read_data_records);

                previous[configured] = stats;
            }

            // Releases the list and the handles in it.
            dxfg_v4_message_connector_list_release(isolateThread, connectors);
        }

        if (useListener) {
            // Remove first, then release: the release waits for a call in progress, so nothing is called afterwards.
            dxfg_v4_DXEndpoint_removeMessageConnectorListener(isolateThread, endpoint, listener);
            dxfg_v4_JavaObjectHandler_release(isolateThread, &listener->handler);
        }

        dxfg_v4_DXFeedSubscription_close(isolateThread, subscription);
        dxfg_v4_DXEndpoint_close(isolateThread, endpoint);
        dxfg_v4_JavaObjectHandler_release(isolateThread, &subscription->handler);
        dxfg_v4_JavaObjectHandler_release(isolateThread, &feed->handler);
        dxfg_v4_JavaObjectHandler_release(isolateThread, &endpoint->handler);

        puts("== MessageConnectors::END ==");
    }};
} // namespace dxfg
