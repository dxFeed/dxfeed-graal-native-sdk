// Copyright (c) 2026 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

// The description of the C API that the build ships next to the library (dxfg-api.json) against the library itself:
// the version, the advanced revision and the features of the description are the ones that the runtime reports.

#include <doctest.h>
#include <dxfg_api.h>

#include "Hub.hpp"

#include <algorithm>
#include <fstream>
#include <regex>
#include <sstream>
#include <string>
#include <vector>

namespace dxfg {
namespace test {

namespace {

std::string readDescription() {
    std::ifstream file(DXFG_TEST_API_DESCRIPTION, std::ios::binary);
    REQUIRE(file.is_open());

    std::ostringstream text;
    text << file.rdbuf();

    return text.str();
}

// The first group of the pattern in the description; fails when the pattern does not match.
std::string find(const std::string &description, const std::string &pattern) {
    std::smatch match;
    REQUIRE(std::regex_search(description, match, std::regex(pattern)));

    return match[1].str();
}

std::vector<std::string> features(const std::string &description) {
    const auto list = find(description, R"re("features": (\[[^\]]*\]))re");
    const std::regex name(R"re("([^"]*)")re");
    std::vector<std::string> names;

    for (auto it = std::sregex_iterator(list.begin(), list.end(), name); it != std::sregex_iterator(); ++it) {
        names.push_back((*it)[1].str());
    }

    return names;
}

} // namespace

TEST_CASE("The description has the version of the library") {
    const auto description = readDescription();
    const Isolate isolate;
    REQUIRE(isolate.thread != nullptr);

    const char *version = nullptr;
    REQUIRE(dxfg_v4_system_get_version(isolate.thread, &version) == DXFG_EXECUTE_SUCCESSFULLY);
    REQUIRE(version != nullptr);
    CHECK(find(description, R"re("sdk_version": "([^"]*)")re") == version);
    REQUIRE(dxfg_v4_String_release(isolate.thread, version) == DXFG_EXECUTE_SUCCESSFULLY);
}

TEST_CASE("The description has the advanced revision of the library") {
    const auto description = readDescription();
    const Isolate isolate;
    REQUIRE(isolate.thread != nullptr);

    int32_t revision = -1;
    REQUIRE(dxfg_v4_system_get_advanced_revision(isolate.thread, &revision) == DXFG_EXECUTE_SUCCESSFULLY);
    CHECK(find(description, R"re("advanced_revision": (-?\d+))re") == std::to_string(revision));
    CHECK(revision == DXFG_V4_ADVANCED_REVISION);
}

TEST_CASE("The library supports every feature of the description") {
    const auto names = features(readDescription());
    const Isolate isolate;
    REQUIRE(isolate.thread != nullptr);

    // The features of the headers are in the description.
    CHECK(std::find(names.begin(), names.end(), DXFG_V4_FEATURE_EVENT_QUEUE) != names.end());
    CHECK(std::find(names.begin(), names.end(), DXFG_V4_FEATURE_SHARED_EVENT_QUEUE) != names.end());
    CHECK(std::find(names.begin(), names.end(), DXFG_V4_FEATURE_EVENT_QUEUE_READY_SIGNAL) != names.end());
    CHECK(std::find(names.begin(), names.end(), DXFG_V4_FEATURE_FEED_FILTER) != names.end());
    CHECK(std::find(names.begin(), names.end(), DXFG_V4_FEATURE_TIME_SERIES_TX_MODEL_EVENT_PEEK) != names.end());
    CHECK(std::find(names.begin(), names.end(), DXFG_V4_FEATURE_MESSAGE_CONNECTORS) != names.end());

    for (const auto &name : names) {
        INFO(name);
        int32_t supported = -1;
        REQUIRE(dxfg_v4_system_is_feature_supported(isolate.thread, name.c_str(), &supported) ==
                DXFG_EXECUTE_SUCCESSFULLY);
        CHECK(supported == 1);
    }
}

TEST_CASE("The ready signal function and its feature are experimental in the description") {
    const auto description = readDescription();

    // The entry of a name: the text from the name to the next entry (the tier is one of its first fields).
    const auto entryOf = [&description](const std::string &name) {
        const auto start = description.find("\"name\": \"" + name + "\"");
        REQUIRE(start != std::string::npos);

        return description.substr(start, 300);
    };

    CHECK(entryOf("dxfg_v4_EventQueue_setReadySignal").find("\"tier\": \"experimental\"") != std::string::npos);
    CHECK(entryOf("DXFG_V4_FEATURE_EVENT_QUEUE_READY_SIGNAL").find("\"tier\": \"experimental\"") != std::string::npos);
    CHECK(entryOf("dxfg_v4_ready_signal_kind_t").find("\"tier\": \"experimental\"") != std::string::npos);
}

TEST_CASE("The feed filter functions, types and feature are experimental in the description") {
    const auto description = readDescription();

    // The entry of a name: the text from the name to the next entry (the tier is one of its first fields).
    const auto entryOf = [&description](const std::string &name) {
        const auto start = description.find("\"name\": \"" + name + "\"");
        REQUIRE(start != std::string::npos);

        return description.substr(start, 300);
    };

    for (const char *name :
         {"dxfg_v4_DXFeedFilter_newBuilder", "dxfg_v4_DXFeedFilter_Builder_withCategory",
          "dxfg_v4_DXFeedFilter_Builder_withCategories", "dxfg_v4_DXFeedFilter_Builder_build",
          "dxfg_v4_DXFeedFilter_accept", "dxfg_v4_DXFeedFilter_acceptEvent", "dxfg_v4_DXFeedFilter_getCategory",
          "dxfg_v4_DXFeedFilter_getEventCategory", "dxfg_v4_DXFeedFilter_isDynamic", "dxfg_v4_DXFeedFilter_getTracker",
          "dxfg_v4_DXFeedFilterTracker_getCurrentFilter", "dxfg_v4_DXFeedFilterTracker_addListener",
          "dxfg_v4_DXFeedFilterTracker_removeListener", "dxfg_v4_DXFeedFilterListener_new",
          "dxfg_v4_feed_filter_builder_t", "dxfg_v4_feed_filter_t", "dxfg_v4_feed_filter_tracker_t",
          "dxfg_v4_feed_filter_listener_t", "dxfg_v4_feed_filter_listener_func", "DXFG_V4_FEATURE_FEED_FILTER"}) {
        INFO(name);
        CHECK(entryOf(name).find("\"tier\": \"experimental\"") != std::string::npos);
    }
}

TEST_CASE("The event peek function is experimental, the filters with user data are stable") {
    const auto description = readDescription();

    // The entry of a name: the text from the name to the next entry (the tier is one of its first fields).
    const auto entryOf = [&description](const std::string &name) {
        const auto start = description.find("\"name\": \"" + name + "\"");
        REQUIRE(start != std::string::npos);

        return description.substr(start, 300);
    };
    const auto experimental = std::string("\"tier\": \"experimental\"");
    const auto stable = std::string("\"tier\": \"stable\"");

    CHECK(entryOf("dxfg_v4_TimeSeriesTxModel_Builder_withEventPeekFunction").find(experimental) != std::string::npos);
    CHECK(entryOf("dxfg_v4_time_series_tx_model_event_peek_function").find(experimental) != std::string::npos);
    CHECK(entryOf("DXFG_V4_FEATURE_TIME_SERIES_TX_MODEL_EVENT_PEEK").find(experimental) != std::string::npos);
    CHECK(entryOf("dxfg_v4_DayFilter_newWithUserData").find(stable) != std::string::npos);
    CHECK(entryOf("dxfg_v4_SessionFilter_newWithUserData").find(stable) != std::string::npos);
}

TEST_CASE("The connector getters and their feature are stable, the connector listener is experimental") {
    const auto description = readDescription();

    // The entry of a name: the text from the name to the next entry (the tier is one of its first fields).
    const auto entryOf = [&description](const std::string &name) {
        const auto start = description.find("\"name\": \"" + name + "\"");
        REQUIRE(start != std::string::npos);

        return description.substr(start, 300);
    };
    const auto experimental = std::string("\"tier\": \"experimental\"");
    const auto stable = std::string("\"tier\": \"stable\"");

    for (const char *name :
         {"dxfg_v4_DXEndpoint_getConnectors", "dxfg_v4_message_connector_list_release",
          "dxfg_v4_MessageConnector_getName", "dxfg_v4_MessageConnector_getKind", "dxfg_v4_MessageConnector_getAddress",
          "dxfg_v4_MessageConnector_getCurrentAddresses", "dxfg_v4_MessageConnector_getState",
          "dxfg_v4_MessageConnector_isActive", "dxfg_v4_MessageConnector_getConnectionCount",
          "dxfg_v4_MessageConnector_getClosedConnectionCount", "dxfg_v4_MessageConnector_getStatistics",
          "dxfg_v4_message_connector_t", "dxfg_v4_message_connector_list", "dxfg_v4_message_connector_stats_t",
          "DXFG_V4_FEATURE_MESSAGE_CONNECTORS"}) {
        INFO(name);
        CHECK(entryOf(name).find(stable) != std::string::npos);
    }

    for (const char *name :
         {"dxfg_v4_MessageConnectorListener_new", "dxfg_v4_DXEndpoint_addMessageConnectorListener",
          "dxfg_v4_DXEndpoint_removeMessageConnectorListener", "dxfg_v4_message_connector_listener_t",
          "dxfg_v4_message_connector_listener_func"}) {
        INFO(name);
        CHECK(entryOf(name).find(experimental) != std::string::npos);
    }
}

} // namespace test
} // namespace dxfg
