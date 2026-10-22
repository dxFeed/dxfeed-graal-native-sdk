// Copyright (c) 2026 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

// The requested aggregation period of an endpoint, the aggregation period info of an endpoint and of a subscription,
// and the dropped events count of a subscription (QDS 3.348 and 3.351).

#ifdef _WIN32
#    ifndef WIN32_LEAN_AND_MEAN
#        define WIN32_LEAN_AND_MEAN
#    endif
#    include <winsock2.h>
#else
#    include <arpa/inet.h>
#    include <netinet/in.h>
#    include <sys/socket.h>
#    include <unistd.h>
#endif

#include <catch.hpp>
#include <dxfg_api.h>

#include <atomic>
#include <chrono>
#include <cstdint>
#include <stdexcept>
#include <string>
#include <thread>

namespace dxfg {
namespace test {

namespace {

#ifdef _WIN32
using NativeSocket = SOCKET;
constexpr NativeSocket INVALID_NATIVE_SOCKET = INVALID_SOCKET;
using SocketAddressLength = int;
#else
using NativeSocket = int;
constexpr NativeSocket INVALID_NATIVE_SOCKET = -1;
using SocketAddressLength = socklen_t;
#endif

// Returns a free loopback port: binds port 0, reads the port the OS picked and closes the socket.
std::uint16_t findFreeLoopbackPort() {
#ifdef _WIN32
    WSADATA wsaData{};

    if (WSAStartup(MAKEWORD(2, 2), &wsaData) != 0) {
        throw std::runtime_error("findFreeLoopbackPort: WSAStartup failed");
    }
#endif

    const NativeSocket s = socket(AF_INET, SOCK_STREAM, IPPROTO_TCP);
    sockaddr_in address{};
    address.sin_family = AF_INET;
    address.sin_addr.s_addr = htonl(INADDR_LOOPBACK);
    address.sin_port = 0; // the OS picks a free port

    SocketAddressLength length = sizeof(address);
    const bool isBound = s != INVALID_NATIVE_SOCKET &&
                         bind(s, reinterpret_cast<sockaddr *>(&address), sizeof(address)) == 0 &&
                         getsockname(s, reinterpret_cast<sockaddr *>(&address), &length) == 0;

#ifdef _WIN32
    if (s != INVALID_NATIVE_SOCKET) {
        closesocket(s);
    }

    WSACleanup();
#else
    if (s != INVALID_NATIVE_SOCKET) {
        close(s);
    }
#endif

    if (!isBound) {
        throw std::runtime_error("findFreeLoopbackPort: socket() or bind() failed");
    }

    return ntohs(address.sin_port);
}

// The server bounds the requested period to [1s, 3s]: a request of 500 ms gets 1 s.
std::string serverAddress(std::uint16_t port) {
    return ":" + std::to_string(port) +
           "[minAggregationPeriod=1s,defaultAggregationPeriod=2s,maxAggregationPeriod=3s]";
}

std::string clientAddress(std::uint16_t port) {
    return "localhost:" + std::to_string(port);
}

graal_isolatethread_t *isolateThread() {
    static graal_isolatethread_t *thread = [] {
        graal_isolate_t *isolate = nullptr;
        graal_isolatethread_t *result = nullptr;

        return graal_create_isolate(nullptr, &isolate, &result) == 0 ? result : nullptr;
    }();

    return thread;
}

// Returns true if the call failed and clears its exception.
bool failed(graal_isolatethread_t *thread, int32_t result) {
    dxfg_exception_t *exception = dxfg_get_and_clear_thread_exception_t(thread);

    dxfg_Exception_release(thread, exception);

    return result != 0 && exception != nullptr;
}

int64_t requestedMillis(graal_isolatethread_t *thread, dxfg_endpoint_t *endpoint) {
    int64_t period = -2;

    REQUIRE(dxfg_DXEndpoint_getRequestedAggregationPeriodMillis(thread, endpoint, &period) == 0);

    return period;
}

void closeAndRelease(graal_isolatethread_t *thread, dxfg_endpoint_t *endpoint) {
    dxfg_DXEndpoint_close(thread, endpoint);
    dxfg_JavaObjectHandler_release(thread, &endpoint->handler);
}

void onEvents(graal_isolatethread_t *, dxfg_event_type_list *, void *userData) {
    static_cast<std::atomic<bool> *>(userData)->store(true);
}

} // namespace

TEST_CASE("The requested aggregation period of an endpoint", "[AggregationPeriod]") {
    graal_isolatethread_t *thread = isolateThread();

    REQUIRE(thread != nullptr);

    dxfg_endpoint_t *endpoint = dxfg_DXEndpoint_create2(thread, DXFG_ENDPOINT_ROLE_FEED);

    REQUIRE(endpoint != nullptr);

    SECTION("Not requested by default") {
        // A non-NULL value shows that the function writes NULL.
        dxfg_time_period_t *zero = dxfg_TimePeriod_ZERO(thread);
        dxfg_time_period_t *period = zero;

        REQUIRE(zero != nullptr);
        REQUIRE(dxfg_DXEndpoint_getRequestedAggregationPeriod(thread, endpoint, &period) == 0);
        CHECK(period == nullptr);
        CHECK(requestedMillis(thread, endpoint) == -1);
        dxfg_JavaObjectHandler_release(thread, &zero->handler);
    }

    SECTION("Set and cleared in millis") {
        REQUIRE(dxfg_DXEndpoint_setRequestedAggregationPeriodMillis(thread, endpoint, 5000) == 0);
        CHECK(requestedMillis(thread, endpoint) == 5000);

        dxfg_time_period_t *period = nullptr;

        REQUIRE(dxfg_DXEndpoint_getRequestedAggregationPeriod(thread, endpoint, &period) == 0);
        REQUIRE(period != nullptr);
        CHECK(dxfg_TimePeriod_getTime(thread, period) == 5000);
        dxfg_JavaObjectHandler_release(thread, &period->handler);

        REQUIRE(dxfg_DXEndpoint_setRequestedAggregationPeriodMillis(thread, endpoint, 0) == 0);
        CHECK(requestedMillis(thread, endpoint) == 0);

        REQUIRE(dxfg_DXEndpoint_setRequestedAggregationPeriodMillis(thread, endpoint, -1) == 0);
        CHECK(requestedMillis(thread, endpoint) == -1);
    }

    SECTION("Set and cleared with a time period") {
        dxfg_time_period_t *period = dxfg_TimePeriod_valueOf(thread, 2000);

        REQUIRE(period != nullptr);
        REQUIRE(dxfg_DXEndpoint_setRequestedAggregationPeriod(thread, endpoint, period) == 0);
        dxfg_JavaObjectHandler_release(thread, &period->handler);
        CHECK(requestedMillis(thread, endpoint) == 2000);

        REQUIRE(dxfg_DXEndpoint_setRequestedAggregationPeriod(thread, endpoint, nullptr) == 0);
        CHECK(requestedMillis(thread, endpoint) == -1);
    }

    SECTION("Errors") {
        REQUIRE(dxfg_DXEndpoint_setRequestedAggregationPeriodMillis(thread, endpoint, 1000) == 0);
        CHECK(failed(thread, dxfg_DXEndpoint_setRequestedAggregationPeriodMillis(thread, endpoint, 86400001)));
        CHECK(requestedMillis(thread, endpoint) == 1000);
        CHECK(failed(thread, dxfg_DXEndpoint_getRequestedAggregationPeriod(thread, endpoint, nullptr)));
        CHECK(failed(thread, dxfg_DXEndpoint_getRequestedAggregationPeriodMillis(thread, endpoint, nullptr)));
    }

    closeAndRelease(thread, endpoint);
}

TEST_CASE("The aggregation period info of an endpoint comes from the server", "[AggregationPeriod]") {
    graal_isolatethread_t *thread = isolateThread();

    REQUIRE(thread != nullptr);

    dxfg_endpoint_t *server = dxfg_DXEndpoint_create2(thread, DXFG_ENDPOINT_ROLE_PUBLISHER);
    dxfg_endpoint_t *client = dxfg_DXEndpoint_create2(thread, DXFG_ENDPOINT_ROLE_FEED);

    REQUIRE(server != nullptr);
    REQUIRE(client != nullptr);

    int64_t min = 0;
    int64_t max = 0;

    REQUIRE(dxfg_DXEndpoint_getAggregationPeriodInfo(thread, client, &min, &max) == 0);
    CHECK(min < 0);
    CHECK(max < 0);
    CHECK(failed(thread, dxfg_DXEndpoint_getAggregationPeriodInfo(thread, client, nullptr, &max)));
    CHECK(failed(thread, dxfg_DXEndpoint_getAggregationPeriodInfo(thread, client, &min, nullptr)));

    const std::uint16_t port = findFreeLoopbackPort();

    REQUIRE(dxfg_DXEndpoint_connect(thread, server, serverAddress(port).c_str()) == 0);
    REQUIRE(dxfg_DXEndpoint_setRequestedAggregationPeriodMillis(thread, client, 500) == 0);
    REQUIRE(dxfg_DXEndpoint_connect(thread, client, clientAddress(port).c_str()) == 0);

    // The server reports the period when the connection is established.
    for (int i = 0; i < 1000; ++i) {
        REQUIRE(dxfg_DXEndpoint_getAggregationPeriodInfo(thread, client, &min, &max) == 0);

        if (min >= 0) {
            break;
        }

        std::this_thread::sleep_for(std::chrono::milliseconds(10));
    }

    CHECK(min == 1000);
    CHECK(max == 1000);

    closeAndRelease(thread, client);
    closeAndRelease(thread, server);
}

TEST_CASE("The aggregation period info and the dropped events of a subscription", "[AggregationPeriod]") {
    graal_isolatethread_t *thread = isolateThread();

    REQUIRE(thread != nullptr);

    dxfg_endpoint_t *hub = dxfg_DXEndpoint_create2(thread, DXFG_ENDPOINT_ROLE_LOCAL_HUB);

    REQUIRE(hub != nullptr);

    dxfg_feed_t *feed = dxfg_DXEndpoint_getFeed(thread, hub);
    dxfg_subscription_t *subscription = dxfg_DXFeed_createSubscription(thread, feed, DXFG_EVENT_QUOTE);

    REQUIRE(subscription != nullptr);

    int64_t min = 0;
    int64_t max = 0;
    int64_t dropped = -1;

    // Unknown before the first events.
    REQUIRE(dxfg_DXFeedSubscription_getAggregationPeriodInfo(thread, subscription, &min, &max) == 0);
    CHECK(min < 0);
    CHECK(max < 0);
    REQUIRE(dxfg_DXFeedSubscription_getTotalDroppedEvents(thread, subscription, &dropped) == 0);
    CHECK(dropped == 0);
    CHECK(failed(thread, dxfg_DXFeedSubscription_getAggregationPeriodInfo(thread, subscription, nullptr, &max)));
    CHECK(failed(thread, dxfg_DXFeedSubscription_getAggregationPeriodInfo(thread, subscription, &min, nullptr)));
    CHECK(failed(thread, dxfg_DXFeedSubscription_getTotalDroppedEvents(thread, subscription, nullptr)));

    std::atomic<bool> received{false};
    dxfg_feed_event_listener_t *listener = dxfg_DXFeedEventListener_new(thread, onEvents, &received);

    REQUIRE(listener != nullptr);
    REQUIRE(dxfg_DXFeedSubscription_addEventListener(thread, subscription, listener) == 0);

    dxfg_string_symbol_t symbol{{STRING}, "AGGREGATION-PERIOD-TEST"};

    REQUIRE(dxfg_DXFeedSubscription_addSymbol(thread, subscription, &symbol.supper) == 0);

    dxfg_quote_t quote{};

    quote.market_event.event_type.clazz = DXFG_EVENT_QUOTE;
    quote.market_event.event_symbol = symbol.symbol;
    quote.bid_price = 1.0;

    dxfg_event_type_t *events[] = {&quote.market_event.event_type};
    dxfg_event_type_list list{1, events};
    dxfg_publisher_t *publisher = dxfg_DXEndpoint_getPublisher(thread, hub);

    REQUIRE(dxfg_DXPublisher_publishEvents(thread, publisher, &list) == 0);
    dxfg_JavaObjectHandler_release(thread, &publisher->handler);

    for (int i = 0; i < 500 && !received.load(); ++i) {
        std::this_thread::sleep_for(std::chrono::milliseconds(10));
    }

    // CHECK, not REQUIRE: the listener refers to `received`, so it is removed below in any case.
    CHECK(received.load());

    // The subscription and the hub do not aggregate the events: the applied period is zero.
    CHECK(dxfg_DXFeedSubscription_getAggregationPeriodInfo(thread, subscription, &min, &max) == 0);
    CHECK(min == 0);
    CHECK(max == 0);
    CHECK(dxfg_DXFeedSubscription_getTotalDroppedEvents(thread, subscription, &dropped) == 0);
    CHECK(dropped == 0);

    dxfg_DXFeedSubscription_removeEventListener(thread, subscription, listener);
    dxfg_JavaObjectHandler_release(thread, &listener->handler);
    dxfg_DXFeedSubscription_close(thread, subscription);
    dxfg_JavaObjectHandler_release(thread, &subscription->handler);
    dxfg_JavaObjectHandler_release(thread, &feed->handler);
    closeAndRelease(thread, hub);
}

} // namespace test
} // namespace dxfg
