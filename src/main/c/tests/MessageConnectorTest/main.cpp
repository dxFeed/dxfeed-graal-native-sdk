// Copyright (c) 2026 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

// The message connectors of an endpoint through the C API (dxfg_v4_message_connector.h): a feed endpoint connected to
// a publisher endpoint on a free loopback port reports its client connector as connected, with its addresses and
// growing statistics; a client to a closed port reports connecting; the functions refuse NULL handles and pointers; the
// lists and the handles outlive the endpoint and are released in any order. A listener gets the state changes on a
// thread of the SDK and is silent once released; dxfg_v4_system_shutdown() waits for its call in progress. A failed
// connect and a connection closed by the server are recorded as the last connection errors of the connector and of
// the endpoint before the listener call; a healthy connection has none and clears none.

#ifdef _WIN32
#    ifndef WIN32_LEAN_AND_MEAN
#        define WIN32_LEAN_AND_MEAN
#    endif
#    include <winsock2.h>
#else
#    include <arpa/inet.h>
#    include <netinet/in.h>
#    include <sys/select.h>
#    include <sys/socket.h>
#    include <unistd.h>
#endif

#include <doctest.h>
#include <dxfg_api.h>

#include "Hub.hpp"

#include <atomic>
#include <chrono>
#include <condition_variable>
#include <cstdint>
#include <functional>
#include <mutex>
#include <stdexcept>
#include <string>
#include <thread>
#include <vector>

namespace dxfg {
namespace test {

namespace {

constexpr auto TIMEOUT = std::chrono::seconds(20);

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

Isolate &isolate() {
    static Isolate instance;

    return instance;
}

// Waits until the condition holds or the timeout passes; returns the last result of the condition.
bool waitFor(const std::function<bool()> &condition) {
    const auto deadline = std::chrono::steady_clock::now() + TIMEOUT;

    while (!condition()) {
        if (std::chrono::steady_clock::now() > deadline) {
            return false;
        }

        std::this_thread::sleep_for(std::chrono::milliseconds(20));
    }

    return true;
}

// An endpoint of the role, closed and released at the end.
struct Endpoint {
    graal_isolatethread_t *thread;
    dxfg_v4_endpoint_t *endpoint = nullptr;
    bool ready = false;

    Endpoint(graal_isolatethread_t *thread, dxfg_endpoint_role_t role) : thread(thread) {
        ready = dxfg_v4_DXEndpoint_createWithRole(thread, role, &endpoint) == DXFG_EXECUTE_SUCCESSFULLY;
    }

    Endpoint(const Endpoint &) = delete;
    Endpoint &operator=(const Endpoint &) = delete;

    ~Endpoint() {
        if (endpoint != nullptr) {
            dxfg_v4_DXEndpoint_close(thread, endpoint);
            dxfg_v4_JavaObjectHandler_release(thread, &endpoint->handler);
        }
    }

    bool connect(const std::string &address) const {
        return dxfg_v4_DXEndpoint_connect(thread, endpoint, address.c_str()) == DXFG_EXECUTE_SUCCESSFULLY;
    }
};

// The connectors of an endpoint, released at the end.
struct Connectors {
    graal_isolatethread_t *thread;
    dxfg_v4_message_connector_list *list = nullptr;

    Connectors(graal_isolatethread_t *thread, dxfg_v4_endpoint_t *endpoint) : thread(thread) {
        REQUIRE(dxfg_v4_DXEndpoint_getConnectors(thread, endpoint, &list) == DXFG_EXECUTE_SUCCESSFULLY);
        REQUIRE(list != nullptr);
    }

    Connectors(const Connectors &) = delete;
    Connectors &operator=(const Connectors &) = delete;

    ~Connectors() {
        dxfg_v4_message_connector_list_release(thread, list);
    }

    int32_t size() const {
        return list->size;
    }

    dxfg_v4_message_connector_t *operator[](int32_t index) const {
        return list->elements[index];
    }
};

std::string takeString(graal_isolatethread_t *thread, const char *value) {
    std::string result = value != nullptr ? value : "<null>";

    dxfg_v4_String_release(thread, value);

    return result;
}

std::string nameOf(graal_isolatethread_t *thread, dxfg_v4_message_connector_t *connector) {
    const char *name = nullptr;

    REQUIRE(dxfg_v4_MessageConnector_getName(thread, connector, &name) == DXFG_EXECUTE_SUCCESSFULLY);

    return takeString(thread, name);
}

std::string addressOf(graal_isolatethread_t *thread, dxfg_v4_message_connector_t *connector) {
    const char *address = nullptr;

    REQUIRE(dxfg_v4_MessageConnector_getAddress(thread, connector, &address) == DXFG_EXECUTE_SUCCESSFULLY);

    return takeString(thread, address);
}

std::vector<std::string> currentAddressesOf(graal_isolatethread_t *thread, dxfg_v4_message_connector_t *connector) {
    dxfg_v4_string_list *addresses = nullptr;

    REQUIRE(dxfg_v4_MessageConnector_getCurrentAddresses(thread, connector, &addresses) ==
            DXFG_EXECUTE_SUCCESSFULLY);
    REQUIRE(addresses != nullptr);

    std::vector<std::string> result;

    for (int32_t i = 0; i < addresses->size; i++) {
        result.emplace_back(addresses->elements[i]);
    }

    dxfg_v4_string_list_release(thread, addresses);

    return result;
}

int32_t intValue(graal_isolatethread_t *thread, dxfg_v4_message_connector_t *connector,
                 int32_t (*getter)(graal_isolatethread_t *, dxfg_v4_message_connector_t *, int32_t *)) {
    int32_t value = -1;

    REQUIRE(getter(thread, connector, &value) == DXFG_EXECUTE_SUCCESSFULLY);

    return value;
}

int32_t stateOf(graal_isolatethread_t *thread, dxfg_v4_message_connector_t *connector) {
    return intValue(thread, connector, &dxfg_v4_MessageConnector_getState);
}

dxfg_v4_message_connector_stats_t statisticsOf(graal_isolatethread_t *thread, dxfg_v4_message_connector_t *connector) {
    dxfg_v4_message_connector_stats_t stats{-1, -1, -1, -1, -1, -1};

    REQUIRE(dxfg_v4_MessageConnector_getStatistics(thread, connector, &stats) == DXFG_EXECUTE_SUCCESSFULLY);

    return stats;
}

// Publishes a quote of the symbol until the feed endpoint has read data records, so that the subscription reaches
// the publisher before the quote does.
bool publishUntilRead(graal_isolatethread_t *thread, dxfg_v4_endpoint_t *publisherEndpoint,
                      dxfg_v4_message_connector_t *client, const char *symbol) {
    dxfg_v4_publisher_t *publisher = nullptr;

    if (dxfg_v4_DXEndpoint_getPublisher(thread, publisherEndpoint, &publisher) != DXFG_EXECUTE_SUCCESSFULLY) {
        return false;
    }

    dxfg_v4_quote_t quote{};
    quote.market_event.event_type.clazz = DXFG_EVENT_QUOTE;
    quote.market_event.event_symbol = symbol;
    quote.bid_price = 1.5;

    dxfg_v4_event_type_t *events[] = {&quote.market_event.event_type};
    dxfg_v4_event_type_list list{1, events};

    const bool read = waitFor([&] {
        dxfg_v4_DXPublisher_publishEvents(thread, publisher, &list);

        return statisticsOf(thread, client).read_data_records > 0;
    });

    dxfg_v4_JavaObjectHandler_release(thread, &publisher->handler);

    return read;
}

// The calls of a connector listener: the state each call read and its thread. The first call blocks until release()
// if `blocking`.
struct StateCalls {
    bool blocking = false;
    std::mutex mutex;
    std::condition_variable released;
    bool isReleased = false;
    std::vector<int32_t> states;
    std::vector<std::thread::id> threads;
    std::atomic<int> count{0};

    static void call(graal_isolatethread_t *thread, dxfg_v4_message_connector_t *connector, void *userData) {
        auto *self = static_cast<StateCalls *>(userData);
        int32_t state = -1;

        dxfg_v4_MessageConnector_getState(thread, connector, &state);

        std::unique_lock lock(self->mutex);
        self->states.push_back(state);
        self->threads.push_back(std::this_thread::get_id());
        const bool first = self->count++ == 0;

        if (self->blocking && first) {
            self->released.wait(lock, [self] { return self->isReleased; });
        }
    }

    void release() {
        std::lock_guard lock(mutex);
        isReleased = true;
        released.notify_all();
    }

    bool saw(int32_t state) {
        std::lock_guard lock(mutex);

        for (const auto value : states) {
            if (value == state) {
                return true;
            }
        }

        return false;
    }

    std::vector<std::thread::id> callThreads() {
        std::lock_guard lock(mutex);

        return threads;
    }
};

// A connector listener of the calls, released at the end.
struct ConnectorListener {
    graal_isolatethread_t *thread;
    dxfg_v4_message_connector_listener_t *listener = nullptr;

    ConnectorListener(graal_isolatethread_t *thread, StateCalls &calls)
        : ConnectorListener(thread, &StateCalls::call, &calls) {
    }

    ConnectorListener(graal_isolatethread_t *thread, dxfg_v4_message_connector_listener_func function, void *userData)
        : thread(thread) {
        REQUIRE(dxfg_v4_MessageConnectorListener_new(thread, function, userData, &listener) ==
                DXFG_EXECUTE_SUCCESSFULLY);
        REQUIRE(listener != nullptr);
    }

    ConnectorListener(const ConnectorListener &) = delete;
    ConnectorListener &operator=(const ConnectorListener &) = delete;

    ~ConnectorListener() {
        release();
    }

    void release() {
        if (listener != nullptr) {
            dxfg_v4_JavaObjectHandler_release(thread, &listener->handler);
            listener = nullptr;
        }
    }
};

// A copy of a connection error; `present` is false when there was none.
struct ErrorCopy {
    bool present = false;
    int64_t time = 0;
    std::string address;
    int32_t category = -1;
    int32_t dropped = -1;
    std::string exceptionClassName;
    std::string message;
    int64_t count = 0;
};

// Copies and releases the error; no doctest macros, so that a listener may call it on a thread of the SDK.
ErrorCopy takeError(graal_isolatethread_t *thread, dxfg_v4_connection_error_t *error) {
    ErrorCopy copy;

    if (error != nullptr) {
        copy.present = true;
        copy.time = error->time;
        copy.address = error->address != nullptr ? error->address : "";
        copy.category = error->category;
        copy.dropped = error->dropped;
        copy.exceptionClassName = error->exception_class_name != nullptr ? error->exception_class_name : "";
        copy.message = error->message != nullptr ? error->message : "";
        copy.count = error->count;
        dxfg_v4_ConnectionError_release(thread, error);
    }

    return copy;
}

ErrorCopy lastErrorOf(graal_isolatethread_t *thread, dxfg_v4_message_connector_t *connector) {
    dxfg_v4_connection_error_t *error = nullptr;

    REQUIRE(dxfg_v4_MessageConnector_getLastError(thread, connector, &error) == DXFG_EXECUTE_SUCCESSFULLY);

    return takeError(thread, error);
}

ErrorCopy lastErrorOf(graal_isolatethread_t *thread, dxfg_v4_endpoint_t *endpoint) {
    dxfg_v4_connection_error_t *error = nullptr;

    REQUIRE(dxfg_v4_DXEndpoint_getLastConnectionError(thread, endpoint, &error) == DXFG_EXECUTE_SUCCESSFULLY);

    return takeError(thread, error);
}

std::int64_t nowMillis() {
    return std::chrono::duration_cast<std::chrono::milliseconds>(
               std::chrono::system_clock::now().time_since_epoch())
        .count();
}

// The categories of the connection errors that a connector listener read in its calls (-1: no error yet).
struct ErrorCalls {
    std::mutex mutex;
    std::vector<int32_t> categories;

    static void call(graal_isolatethread_t *thread, dxfg_v4_message_connector_t *connector, void *userData) {
        auto *self = static_cast<ErrorCalls *>(userData);
        dxfg_v4_connection_error_t *error = nullptr;
        int32_t category = -1;

        if (dxfg_v4_MessageConnector_getLastError(thread, connector, &error) == DXFG_EXECUTE_SUCCESSFULLY) {
            category = takeError(thread, error).category;
        }

        std::lock_guard lock(self->mutex);
        self->categories.push_back(category);
    }

    bool saw(int32_t category) {
        std::lock_guard lock(mutex);

        for (const auto value : categories) {
            if (value == category) {
                return true;
            }
        }

        return false;
    }
};

void closeSocket(NativeSocket s) {
#ifdef _WIN32
    closesocket(s);
#else
    close(s);
#endif
}

// A loopback server that closes the output of every connection it accepts, so that the client reads the end of the
// stream; it keeps the connections open until the end.
struct ClosingServer {
    NativeSocket listening = INVALID_NATIVE_SOCKET;
    std::uint16_t port = 0;
    std::atomic<bool> stopping{false};
    std::vector<NativeSocket> accepted;
    std::thread thread;

    ClosingServer() {
#ifdef _WIN32
        WSADATA wsaData{};

        if (WSAStartup(MAKEWORD(2, 2), &wsaData) != 0) {
            throw std::runtime_error("ClosingServer: WSAStartup failed");
        }
#endif

        listening = socket(AF_INET, SOCK_STREAM, IPPROTO_TCP);
        sockaddr_in address{};
        address.sin_family = AF_INET;
        address.sin_addr.s_addr = htonl(INADDR_LOOPBACK);
        address.sin_port = 0; // the OS picks a free port

        SocketAddressLength length = sizeof(address);

        if (listening == INVALID_NATIVE_SOCKET ||
            bind(listening, reinterpret_cast<sockaddr *>(&address), sizeof(address)) != 0 ||
            listen(listening, 8) != 0 || getsockname(listening, reinterpret_cast<sockaddr *>(&address), &length) != 0) {
            stop();

            throw std::runtime_error("ClosingServer: socket(), bind() or listen() failed");
        }

        port = ntohs(address.sin_port);
        thread = std::thread([this] { run(); });
    }

    ClosingServer(const ClosingServer &) = delete;
    ClosingServer &operator=(const ClosingServer &) = delete;

    ~ClosingServer() {
        stopping = true;

        if (thread.joinable()) {
            thread.join();
        }

        for (const auto s : accepted) {
            closeSocket(s);
        }

        stop();
    }

  private:
    void run() {
        while (!stopping) {
            fd_set readable;
            FD_ZERO(&readable);
            FD_SET(listening, &readable);
            timeval timeout{0, 100000};

            // The first argument is ignored on Windows.
            if (select(static_cast<int>(listening) + 1, &readable, nullptr, nullptr, &timeout) > 0) {
                const NativeSocket s = accept(listening, nullptr, nullptr);

                if (s != INVALID_NATIVE_SOCKET) {
#ifdef _WIN32
                    shutdown(s, SD_SEND);
#else
                    shutdown(s, SHUT_WR);
#endif
                    accepted.push_back(s);
                }
            }
        }
    }

    void stop() {
        if (listening != INVALID_NATIVE_SOCKET) {
            closeSocket(listening);
            listening = INVALID_NATIVE_SOCKET;
        }

#ifdef _WIN32
        WSACleanup();
#endif
    }
};

} // namespace

TEST_CASE("A feed endpoint connected to a publisher reports its client connector") {
    const auto thread = isolate().thread;
    REQUIRE(thread != nullptr);

    const auto port = std::to_string(findFreeLoopbackPort());
    Endpoint publisher(thread, DXFG_ENDPOINT_ROLE_PUBLISHER);
    REQUIRE(publisher.ready);
    REQUIRE(publisher.connect(":" + port));

    Connectors servers(thread, publisher.endpoint);
    REQUIRE(servers.size() == 1);
    CHECK(intValue(thread, servers[0], &dxfg_v4_MessageConnector_getKind) ==
          DXFG_V4_MESSAGE_CONNECTOR_KIND_SERVER_SOCKET);
    CHECK(addressOf(thread, servers[0]) == "*:" + port); // QD names the address `:port` so.
    REQUIRE(waitFor([&] { return stateOf(thread, servers[0]) == DXFG_V4_MESSAGE_CONNECTOR_STATE_CONNECTED; }));
    CHECK(currentAddressesOf(thread, servers[0]).empty());

    Endpoint feed(thread, DXFG_ENDPOINT_ROLE_FEED);
    REQUIRE(feed.ready);
    REQUIRE(feed.connect("127.0.0.1:" + port));

    Connectors clients(thread, feed.endpoint);
    REQUIRE(clients.size() == 1);
    const auto client = clients[0];

    CHECK(intValue(thread, client, &dxfg_v4_MessageConnector_getKind) == DXFG_V4_MESSAGE_CONNECTOR_KIND_CLIENT_SOCKET);
    CHECK(addressOf(thread, client) == "127.0.0.1:" + port);
    CHECK_FALSE(nameOf(thread, client).empty());
    CHECK(intValue(thread, client, &dxfg_v4_MessageConnector_isActive) == 1);
    REQUIRE(waitFor([&] { return stateOf(thread, client) == DXFG_V4_MESSAGE_CONNECTOR_STATE_CONNECTED; }));
    CHECK(intValue(thread, client, &dxfg_v4_MessageConnector_getConnectionCount) == 1);
    CHECK(currentAddressesOf(thread, client) == std::vector<std::string>{"127.0.0.1:" + port});
    CHECK(waitFor([&] { return intValue(thread, servers[0], &dxfg_v4_MessageConnector_getConnectionCount) == 1; }));

    // A subscription makes the publisher send data.
    dxfg_v4_feed_t *javaFeed = nullptr;
    dxfg_v4_subscription_t *subscription = nullptr;
    dxfg_v4_string_symbol_t symbol{{STRING}, "CONNECTOR-TEST"};
    REQUIRE(dxfg_v4_DXEndpoint_getFeed(thread, feed.endpoint, &javaFeed) == DXFG_EXECUTE_SUCCESSFULLY);
    REQUIRE(dxfg_v4_DXFeed_createSubscription(thread, javaFeed, DXFG_EVENT_QUOTE, &subscription) ==
            DXFG_EXECUTE_SUCCESSFULLY);
    REQUIRE(dxfg_v4_DXFeedSubscription_addSymbol(thread, subscription, &symbol.supper) == DXFG_EXECUTE_SUCCESSFULLY);

    CHECK(publishUntilRead(thread, publisher.endpoint, client, symbol.symbol));

    const auto stats = statisticsOf(thread, client);
    CHECK(stats.read_bytes > 0);
    CHECK(stats.written_bytes > 0);
    CHECK(stats.read_data_records > 0);
    CHECK(stats.written_data_records >= 0);
    CHECK(stats.active_connection_count == 1);
    CHECK(stats.closed_connection_count == 0);
    CHECK(statisticsOf(thread, servers[0]).written_data_records > 0);

    // Reading resets nothing.
    CHECK(statisticsOf(thread, client).read_data_records >= stats.read_data_records);

    dxfg_v4_DXFeedSubscription_close(thread, subscription);
    dxfg_v4_JavaObjectHandler_release(thread, &subscription->handler);
    dxfg_v4_JavaObjectHandler_release(thread, &javaFeed->handler);
    CHECK(takeExceptionClassName(thread).empty());
}

TEST_CASE("A client to a closed port is connecting and has no current address") {
    const auto thread = isolate().thread;
    REQUIRE(thread != nullptr);

    Endpoint feed(thread, DXFG_ENDPOINT_ROLE_FEED);
    REQUIRE(feed.ready);

    Connectors none(thread, feed.endpoint);
    CHECK(none.size() == 0);

    REQUIRE(feed.connect("127.0.0.1:1"));

    Connectors connectors(thread, feed.endpoint);
    REQUIRE(connectors.size() == 1);
    CHECK(stateOf(thread, connectors[0]) == DXFG_V4_MESSAGE_CONNECTOR_STATE_CONNECTING);
    CHECK(intValue(thread, connectors[0], &dxfg_v4_MessageConnector_getConnectionCount) == 0);
    CHECK(currentAddressesOf(thread, connectors[0]).empty());

    int64_t closed = -1;
    CHECK(dxfg_v4_MessageConnector_getClosedConnectionCount(thread, connectors[0], &closed) ==
          DXFG_EXECUTE_SUCCESSFULLY);
    CHECK(closed >= 0);
}

TEST_CASE("A multi-address endpoint has a connector per address") {
    const auto thread = isolate().thread;
    REQUIRE(thread != nullptr);

    Endpoint feed(thread, DXFG_ENDPOINT_ROLE_FEED);
    REQUIRE(feed.ready);
    REQUIRE(feed.connect("(127.0.0.1:1)(127.0.0.1:2)"));

    Connectors connectors(thread, feed.endpoint);
    REQUIRE(connectors.size() == 2);
    CHECK(addressOf(thread, connectors[0]) == "127.0.0.1:1");
    CHECK(addressOf(thread, connectors[1]) == "127.0.0.1:2");
}

TEST_CASE("The functions refuse NULL handles and NULL out pointers") {
    const auto thread = isolate().thread;
    REQUIRE(thread != nullptr);

    const std::string illegalArgument = "java.lang.IllegalArgumentException";
    const auto fails = [&](int32_t result) {
        return result == DXFG_EXECUTE_FAIL && takeExceptionClassName(thread) == illegalArgument;
    };

    Endpoint feed(thread, DXFG_ENDPOINT_ROLE_FEED);
    REQUIRE(feed.ready);
    REQUIRE(feed.connect("127.0.0.1:1"));

    Connectors connectors(thread, feed.endpoint);
    REQUIRE(connectors.size() == 1);
    const auto connector = connectors[0];

    dxfg_v4_message_connector_list *list = nullptr;
    const char *string = nullptr;
    dxfg_v4_string_list *strings = nullptr;
    int32_t value = -1;
    int64_t count = -1;
    dxfg_v4_message_connector_stats_t stats{};

    CHECK(fails(dxfg_v4_DXEndpoint_getConnectors(thread, nullptr, &list)));
    CHECK(fails(dxfg_v4_DXEndpoint_getConnectors(thread, feed.endpoint, nullptr)));
    CHECK(list == nullptr);
    CHECK(fails(dxfg_v4_MessageConnector_getName(thread, nullptr, &string)));
    CHECK(fails(dxfg_v4_MessageConnector_getName(thread, connector, nullptr)));
    CHECK(fails(dxfg_v4_MessageConnector_getKind(thread, nullptr, &value)));
    CHECK(fails(dxfg_v4_MessageConnector_getKind(thread, connector, nullptr)));
    CHECK(fails(dxfg_v4_MessageConnector_getAddress(thread, nullptr, &string)));
    CHECK(fails(dxfg_v4_MessageConnector_getAddress(thread, connector, nullptr)));
    CHECK(fails(dxfg_v4_MessageConnector_getCurrentAddresses(thread, nullptr, &strings)));
    CHECK(fails(dxfg_v4_MessageConnector_getCurrentAddresses(thread, connector, nullptr)));
    CHECK(fails(dxfg_v4_MessageConnector_getState(thread, nullptr, &value)));
    CHECK(fails(dxfg_v4_MessageConnector_getState(thread, connector, nullptr)));
    CHECK(fails(dxfg_v4_MessageConnector_isActive(thread, nullptr, &value)));
    CHECK(fails(dxfg_v4_MessageConnector_isActive(thread, connector, nullptr)));
    CHECK(fails(dxfg_v4_MessageConnector_getConnectionCount(thread, nullptr, &value)));
    CHECK(fails(dxfg_v4_MessageConnector_getConnectionCount(thread, connector, nullptr)));
    CHECK(fails(dxfg_v4_MessageConnector_getClosedConnectionCount(thread, nullptr, &count)));
    CHECK(fails(dxfg_v4_MessageConnector_getClosedConnectionCount(thread, connector, nullptr)));
    CHECK(fails(dxfg_v4_MessageConnector_getStatistics(thread, nullptr, &stats)));
    CHECK(fails(dxfg_v4_MessageConnector_getStatistics(thread, connector, nullptr)));
    CHECK(string == nullptr);
    CHECK(strings == nullptr);
    CHECK(value == -1);
    CHECK(count == -1);

    CHECK(dxfg_v4_message_connector_list_release(thread, nullptr) == DXFG_EXECUTE_SUCCESSFULLY);
}

TEST_CASE("The lists and the handles outlive the endpoint and are released in any order") {
    const auto thread = isolate().thread;
    REQUIRE(thread != nullptr);

    dxfg_v4_message_connector_list *first = nullptr;
    dxfg_v4_message_connector_list *second = nullptr;
    dxfg_v4_java_object_handler *kept = nullptr;
    dxfg_v4_java_object_handler *keptAfterList = nullptr;

    {
        Endpoint feed(thread, DXFG_ENDPOINT_ROLE_FEED);
        REQUIRE(feed.ready);
        REQUIRE(feed.connect("127.0.0.1:1"));
        REQUIRE(dxfg_v4_DXEndpoint_getConnectors(thread, feed.endpoint, &first) == DXFG_EXECUTE_SUCCESSFULLY);
        REQUIRE(dxfg_v4_DXEndpoint_getConnectors(thread, feed.endpoint, &second) == DXFG_EXECUTE_SUCCESSFULLY);
        REQUIRE(first->size == 1);
        REQUIRE(second->size == 1);
        REQUIRE(dxfg_v4_JavaObjectHandler_clone(thread, &first->elements[0]->handler, &kept) ==
                DXFG_EXECUTE_SUCCESSFULLY);
        REQUIRE(dxfg_v4_JavaObjectHandler_clone(thread, &second->elements[0]->handler, &keptAfterList) ==
                DXFG_EXECUTE_SUCCESSFULLY);

        // The handle is released before its list.
        CHECK(dxfg_v4_JavaObjectHandler_release(thread, kept) == DXFG_EXECUTE_SUCCESSFULLY);
    }

    // The endpoint is closed: an empty list, and the old connectors answer as stopped.
    CHECK(stateOf(thread, second->elements[0]) == DXFG_V4_MESSAGE_CONNECTOR_STATE_DISCONNECTED);
    CHECK(intValue(thread, second->elements[0], &dxfg_v4_MessageConnector_isActive) == 0);
    statisticsOf(thread, second->elements[0]);

    CHECK(dxfg_v4_message_connector_list_release(thread, first) == DXFG_EXECUTE_SUCCESSFULLY);
    CHECK(dxfg_v4_message_connector_list_release(thread, second) == DXFG_EXECUTE_SUCCESSFULLY);

    // The handle cloned from a list stays valid after the list is released.
    auto connector = reinterpret_cast<dxfg_v4_message_connector_t *>(keptAfterList);
    CHECK(stateOf(thread, connector) == DXFG_V4_MESSAGE_CONNECTOR_STATE_DISCONNECTED);
    CHECK(dxfg_v4_JavaObjectHandler_release(thread, keptAfterList) == DXFG_EXECUTE_SUCCESSFULLY);
    CHECK(takeExceptionClassName(thread).empty());
}

TEST_CASE("A connector listener gets the state changes on a thread of the SDK and is silent once released") {
    const auto thread = isolate().thread;
    REQUIRE(thread != nullptr);

    const auto port = std::to_string(findFreeLoopbackPort());
    Endpoint publisher(thread, DXFG_ENDPOINT_ROLE_PUBLISHER);
    Endpoint feed(thread, DXFG_ENDPOINT_ROLE_FEED);
    REQUIRE(publisher.ready);
    REQUIRE(feed.ready);
    REQUIRE(publisher.connect(":" + port));

    static StateCalls calls{}; // Static: the listener may outlive the test case if it fails.
    ConnectorListener listener(thread, calls);

    REQUIRE(dxfg_v4_DXEndpoint_addMessageConnectorListener(thread, feed.endpoint, listener.listener) ==
            DXFG_EXECUTE_SUCCESSFULLY);
    // A second add changes nothing.
    REQUIRE(dxfg_v4_DXEndpoint_addMessageConnectorListener(thread, feed.endpoint, listener.listener) ==
            DXFG_EXECUTE_SUCCESSFULLY);
    REQUIRE(feed.connect("127.0.0.1:" + port));
    CHECK(waitFor([] { return calls.saw(DXFG_V4_MESSAGE_CONNECTOR_STATE_CONNECTED); }));

    for (const auto &id : calls.callThreads()) {
        CHECK(id != std::this_thread::get_id());
    }

    REQUIRE(dxfg_v4_DXEndpoint_removeMessageConnectorListener(thread, feed.endpoint, listener.listener) ==
            DXFG_EXECUTE_SUCCESSFULLY);
    listener.release();

    const int count = calls.count;

    REQUIRE(dxfg_v4_DXEndpoint_reconnect(thread, feed.endpoint) == DXFG_EXECUTE_SUCCESSFULLY);
    std::this_thread::sleep_for(std::chrono::seconds(1));
    CHECK(calls.count == count);
}

TEST_CASE("A connector listener added to a connected endpoint gets only the later changes") {
    const auto thread = isolate().thread;
    REQUIRE(thread != nullptr);

    const auto port = std::to_string(findFreeLoopbackPort());
    Endpoint publisher(thread, DXFG_ENDPOINT_ROLE_PUBLISHER);
    Endpoint feed(thread, DXFG_ENDPOINT_ROLE_FEED);
    REQUIRE(publisher.ready);
    REQUIRE(feed.ready);
    REQUIRE(publisher.connect(":" + port));
    REQUIRE(feed.connect("127.0.0.1:" + port));

    Connectors clients(thread, feed.endpoint);
    REQUIRE(clients.size() == 1);
    REQUIRE(waitFor([&] { return stateOf(thread, clients[0]) == DXFG_V4_MESSAGE_CONNECTOR_STATE_CONNECTED; }));

    // Static: the listeners may outlive the test case if it fails.
    static StateCalls firstCalls{};
    static StateCalls secondCalls{};
    ConnectorListener first(thread, firstCalls);
    ConnectorListener second(thread, secondCalls);

    REQUIRE(dxfg_v4_DXEndpoint_addMessageConnectorListener(thread, feed.endpoint, first.listener) ==
            DXFG_EXECUTE_SUCCESSFULLY);
    REQUIRE(dxfg_v4_DXEndpoint_addMessageConnectorListener(thread, feed.endpoint, second.listener) ==
            DXFG_EXECUTE_SUCCESSFULLY);
    std::this_thread::sleep_for(std::chrono::seconds(1));
    CHECK(firstCalls.count == 0);
    CHECK(secondCalls.count == 0);

    REQUIRE(dxfg_v4_DXEndpoint_reconnect(thread, feed.endpoint) == DXFG_EXECUTE_SUCCESSFULLY);
    CHECK(waitFor([] { return firstCalls.count > 0 && secondCalls.count > 0; }));

    REQUIRE(dxfg_v4_DXEndpoint_removeMessageConnectorListener(thread, feed.endpoint, first.listener) ==
            DXFG_EXECUTE_SUCCESSFULLY);
    REQUIRE(dxfg_v4_DXEndpoint_removeMessageConnectorListener(thread, feed.endpoint, second.listener) ==
            DXFG_EXECUTE_SUCCESSFULLY);
    CHECK(takeExceptionClassName(thread).empty());
}

TEST_CASE("The listener functions refuse NULL arguments; a remove of a listener that is not added succeeds") {
    const auto thread = isolate().thread;
    REQUIRE(thread != nullptr);

    Endpoint feed(thread, DXFG_ENDPOINT_ROLE_FEED);
    REQUIRE(feed.ready);

    static StateCalls calls{};
    dxfg_v4_message_connector_listener_t *none = nullptr;

    CHECK(dxfg_v4_MessageConnectorListener_new(thread, nullptr, &calls, &none) == DXFG_EXECUTE_FAIL);
    CHECK(takeExceptionClassName(thread) == "java.lang.IllegalArgumentException");
    CHECK(dxfg_v4_MessageConnectorListener_new(thread, &StateCalls::call, &calls, nullptr) == DXFG_EXECUTE_FAIL);
    CHECK(takeExceptionClassName(thread) == "java.lang.IllegalArgumentException");
    CHECK(none == nullptr);

    ConnectorListener listener(thread, calls);

    CHECK(dxfg_v4_DXEndpoint_addMessageConnectorListener(thread, nullptr, listener.listener) == DXFG_EXECUTE_FAIL);
    CHECK(takeExceptionClassName(thread) == "java.lang.IllegalArgumentException");
    CHECK(dxfg_v4_DXEndpoint_addMessageConnectorListener(thread, feed.endpoint, nullptr) == DXFG_EXECUTE_FAIL);
    CHECK(takeExceptionClassName(thread) == "java.lang.IllegalArgumentException");
    CHECK(dxfg_v4_DXEndpoint_removeMessageConnectorListener(thread, nullptr, listener.listener) ==
          DXFG_EXECUTE_FAIL);
    CHECK(takeExceptionClassName(thread) == "java.lang.IllegalArgumentException");
    CHECK(dxfg_v4_DXEndpoint_removeMessageConnectorListener(thread, feed.endpoint, nullptr) == DXFG_EXECUTE_FAIL);
    CHECK(takeExceptionClassName(thread) == "java.lang.IllegalArgumentException");
    CHECK(dxfg_v4_DXEndpoint_removeMessageConnectorListener(thread, feed.endpoint, listener.listener) ==
          DXFG_EXECUTE_SUCCESSFULLY);
}

TEST_CASE("A failed connect is a connect-failed error, recorded before the listener call; the endpoint has it too") {
    const auto thread = isolate().thread;
    REQUIRE(thread != nullptr);

    Endpoint feed(thread, DXFG_ENDPOINT_ROLE_FEED);
    REQUIRE(feed.ready);
    CHECK_FALSE(lastErrorOf(thread, feed.endpoint).present);

    static ErrorCalls calls{}; // Static: the listener may outlive the test case if it fails.
    ConnectorListener listener(thread, &ErrorCalls::call, &calls);

    REQUIRE(dxfg_v4_DXEndpoint_addMessageConnectorListener(thread, feed.endpoint, listener.listener) ==
            DXFG_EXECUTE_SUCCESSFULLY);

    const auto before = nowMillis();

    REQUIRE(feed.connect("127.0.0.1:1"));
    CHECK(waitFor([] { return calls.saw(DXFG_V4_CONNECTION_ERROR_CONNECT_FAILED); }));

    Connectors connectors(thread, feed.endpoint);
    REQUIRE(connectors.size() == 1);

    const auto error = lastErrorOf(thread, connectors[0]);
    REQUIRE(error.present);
    CHECK(error.category == DXFG_V4_CONNECTION_ERROR_CONNECT_FAILED);
    CHECK(error.time >= before);
    CHECK(error.time <= nowMillis());
    CHECK(error.address == "127.0.0.1:1");
    CHECK(error.dropped == 0);
    CHECK(error.exceptionClassName == "java.net.ConnectException");
    CHECK(error.count >= 1);

    const auto endpointError = lastErrorOf(thread, feed.endpoint);
    REQUIRE(endpointError.present);
    CHECK(endpointError.category == DXFG_V4_CONNECTION_ERROR_CONNECT_FAILED);
    CHECK(endpointError.address == "127.0.0.1:1");

    REQUIRE(dxfg_v4_DXEndpoint_removeMessageConnectorListener(thread, feed.endpoint, listener.listener) ==
            DXFG_EXECUTE_SUCCESSFULLY);
    CHECK(takeExceptionClassName(thread).empty());
}

TEST_CASE("A connection that the server closes is a dropped connection, closed by remote") {
    const auto thread = isolate().thread;
    REQUIRE(thread != nullptr);

    ClosingServer server;
    Endpoint feed(thread, DXFG_ENDPOINT_ROLE_FEED);
    REQUIRE(feed.ready);

    static ErrorCalls calls{}; // Static: the listener may outlive the test case if it fails.
    ConnectorListener listener(thread, &ErrorCalls::call, &calls);

    REQUIRE(dxfg_v4_DXEndpoint_addMessageConnectorListener(thread, feed.endpoint, listener.listener) ==
            DXFG_EXECUTE_SUCCESSFULLY);
    REQUIRE(feed.connect("127.0.0.1:" + std::to_string(server.port)));
    CHECK(waitFor([] { return calls.saw(DXFG_V4_CONNECTION_ERROR_CLOSED_BY_REMOTE); }));

    const auto error = lastErrorOf(thread, feed.endpoint);
    REQUIRE(error.present);
    CHECK(error.category == DXFG_V4_CONNECTION_ERROR_CLOSED_BY_REMOTE);
    CHECK(error.dropped == 1);
    CHECK(error.exceptionClassName == "java.io.EOFException");

    REQUIRE(dxfg_v4_DXEndpoint_removeMessageConnectorListener(thread, feed.endpoint, listener.listener) ==
            DXFG_EXECUTE_SUCCESSFULLY);
    CHECK(takeExceptionClassName(thread).empty());
}

TEST_CASE("A healthy connection has no error; the endpoint keeps an error after a connect to another address") {
    const auto thread = isolate().thread;
    REQUIRE(thread != nullptr);

    const auto port = std::to_string(findFreeLoopbackPort());
    Endpoint publisher(thread, DXFG_ENDPOINT_ROLE_PUBLISHER);
    Endpoint feed(thread, DXFG_ENDPOINT_ROLE_FEED);
    REQUIRE(publisher.ready);
    REQUIRE(feed.ready);
    REQUIRE(publisher.connect(":" + port));
    REQUIRE(feed.connect("127.0.0.1:" + port));

    {
        Connectors clients(thread, feed.endpoint);
        REQUIRE(clients.size() == 1);
        REQUIRE(waitFor([&] { return stateOf(thread, clients[0]) == DXFG_V4_MESSAGE_CONNECTOR_STATE_CONNECTED; }));
        CHECK_FALSE(lastErrorOf(thread, clients[0]).present);
        CHECK_FALSE(lastErrorOf(thread, feed.endpoint).present);
    }

    REQUIRE(feed.connect("127.0.0.1:1"));
    REQUIRE(waitFor([&] { return lastErrorOf(thread, feed.endpoint).present; }));

    REQUIRE(feed.connect("127.0.0.1:" + port));

    Connectors clients(thread, feed.endpoint);
    REQUIRE(clients.size() == 1);
    REQUIRE(waitFor([&] { return stateOf(thread, clients[0]) == DXFG_V4_MESSAGE_CONNECTOR_STATE_CONNECTED; }));
    CHECK_FALSE(lastErrorOf(thread, clients[0]).present);

    // A successful connection clears nothing.
    const auto kept = lastErrorOf(thread, feed.endpoint);
    REQUIRE(kept.present);
    CHECK(kept.category == DXFG_V4_CONNECTION_ERROR_CONNECT_FAILED);
    CHECK(kept.address == "127.0.0.1:1");
}

TEST_CASE("The connection error functions refuse NULL arguments; a release of NULL succeeds") {
    const auto thread = isolate().thread;
    REQUIRE(thread != nullptr);

    const std::string illegalArgument = "java.lang.IllegalArgumentException";
    const auto fails = [&](int32_t result) {
        return result == DXFG_EXECUTE_FAIL && takeExceptionClassName(thread) == illegalArgument;
    };

    Endpoint feed(thread, DXFG_ENDPOINT_ROLE_FEED);
    REQUIRE(feed.ready);
    REQUIRE(feed.connect("127.0.0.1:1"));

    Connectors connectors(thread, feed.endpoint);
    REQUIRE(connectors.size() == 1);

    dxfg_v4_connection_error_t *error = nullptr;

    CHECK(fails(dxfg_v4_MessageConnector_getLastError(thread, nullptr, &error)));
    CHECK(fails(dxfg_v4_MessageConnector_getLastError(thread, connectors[0], nullptr)));
    CHECK(fails(dxfg_v4_DXEndpoint_getLastConnectionError(thread, nullptr, &error)));
    CHECK(fails(dxfg_v4_DXEndpoint_getLastConnectionError(thread, feed.endpoint, nullptr)));
    CHECK(error == nullptr);

    CHECK(dxfg_v4_ConnectionError_release(thread, nullptr) == DXFG_EXECUTE_SUCCESSFULLY);
    CHECK(takeExceptionClassName(thread).empty());
}

TEST_CASE("A connector of a disconnected endpoint is stopped and keeps its name, kind, address and statistics") {
    const auto thread = isolate().thread;
    REQUIRE(thread != nullptr);

    const auto port = std::to_string(findFreeLoopbackPort());
    Endpoint publisher(thread, DXFG_ENDPOINT_ROLE_PUBLISHER);
    REQUIRE(publisher.ready);
    REQUIRE(publisher.connect(":" + port));

    Endpoint feed(thread, DXFG_ENDPOINT_ROLE_FEED);
    REQUIRE(feed.ready);
    REQUIRE(feed.connect("127.0.0.1:" + port));

    Connectors connectors(thread, feed.endpoint);
    REQUIRE(connectors.size() == 1);
    const auto connector = connectors[0];

    REQUIRE(waitFor([&] { return stateOf(thread, connector) == DXFG_V4_MESSAGE_CONNECTOR_STATE_CONNECTED; }));
    const auto name = nameOf(thread, connector);
    const auto stats = statisticsOf(thread, connector);

    REQUIRE(dxfg_v4_DXEndpoint_disconnect(thread, feed.endpoint) == DXFG_EXECUTE_SUCCESSFULLY);

    Connectors none(thread, feed.endpoint);
    CHECK(none.size() == 0);

    CHECK(waitFor([&] { return stateOf(thread, connector) == DXFG_V4_MESSAGE_CONNECTOR_STATE_DISCONNECTED; }));
    CHECK(intValue(thread, connector, &dxfg_v4_MessageConnector_isActive) == 0);
    CHECK(waitFor([&] { return intValue(thread, connector, &dxfg_v4_MessageConnector_getConnectionCount) == 0; }));
    CHECK(currentAddressesOf(thread, connector).empty());
    CHECK(nameOf(thread, connector) == name);
    CHECK(intValue(thread, connector, &dxfg_v4_MessageConnector_getKind) ==
          DXFG_V4_MESSAGE_CONNECTOR_KIND_CLIENT_SOCKET);
    CHECK(addressOf(thread, connector) == "127.0.0.1:" + port);

    const auto later = statisticsOf(thread, connector);
    CHECK(later.read_bytes >= stats.read_bytes);
    CHECK(later.written_bytes >= stats.written_bytes);
    CHECK(later.read_data_records >= stats.read_data_records);
    CHECK(takeExceptionClassName(thread).empty());
}

TEST_CASE("A connector replaced by a connect to another address is stopped and keeps its address") {
    const auto thread = isolate().thread;
    REQUIRE(thread != nullptr);

    Endpoint feed(thread, DXFG_ENDPOINT_ROLE_FEED);
    REQUIRE(feed.ready);
    REQUIRE(feed.connect("127.0.0.1:1"));

    Connectors first(thread, feed.endpoint);
    REQUIRE(first.size() == 1);
    const auto replaced = first[0];

    REQUIRE(feed.connect("127.0.0.1:2"));

    Connectors second(thread, feed.endpoint);
    REQUIRE(second.size() == 1);
    CHECK(addressOf(thread, second[0]) == "127.0.0.1:2");

    CHECK(waitFor([&] { return stateOf(thread, replaced) == DXFG_V4_MESSAGE_CONNECTOR_STATE_DISCONNECTED; }));
    CHECK(intValue(thread, replaced, &dxfg_v4_MessageConnector_isActive) == 0);
    CHECK(intValue(thread, replaced, &dxfg_v4_MessageConnector_getConnectionCount) == 0);
    CHECK(currentAddressesOf(thread, replaced).empty());
    CHECK(addressOf(thread, replaced) == "127.0.0.1:1");
    CHECK(takeExceptionClassName(thread).empty());
}

TEST_CASE("A connector listener added to a closed endpoint is accepted and never called") {
    const auto thread = isolate().thread;
    REQUIRE(thread != nullptr);

    Endpoint feed(thread, DXFG_ENDPOINT_ROLE_FEED);
    REQUIRE(feed.ready);
    REQUIRE(dxfg_v4_DXEndpoint_close(thread, feed.endpoint) == DXFG_EXECUTE_SUCCESSFULLY);

    static StateCalls calls{}; // Static: the listener may outlive the test case if it fails.
    ConnectorListener listener(thread, calls);

    CHECK(dxfg_v4_DXEndpoint_addMessageConnectorListener(thread, feed.endpoint, listener.listener) ==
          DXFG_EXECUTE_SUCCESSFULLY);
    CHECK(takeExceptionClassName(thread).empty());

    // A connect of a closed endpoint succeeds and creates no connector.
    CHECK(feed.connect("127.0.0.1:1"));
    CHECK(takeExceptionClassName(thread).empty());

    Connectors none(thread, feed.endpoint);
    CHECK(none.size() == 0);

    std::this_thread::sleep_for(std::chrono::milliseconds(500));
    CHECK(calls.count == 0);
}

TEST_CASE("A connector of a closed endpoint keeps its last connection error") {
    const auto thread = isolate().thread;
    REQUIRE(thread != nullptr);

    Endpoint feed(thread, DXFG_ENDPOINT_ROLE_FEED);
    REQUIRE(feed.ready);
    REQUIRE(feed.connect("127.0.0.1:1"));

    Connectors connectors(thread, feed.endpoint);
    REQUIRE(connectors.size() == 1);
    const auto connector = connectors[0];

    REQUIRE(waitFor([&] { return lastErrorOf(thread, connector).present; }));
    const auto before = lastErrorOf(thread, connector);

    REQUIRE(dxfg_v4_DXEndpoint_close(thread, feed.endpoint) == DXFG_EXECUTE_SUCCESSFULLY);
    REQUIRE(waitFor([&] { return stateOf(thread, connector) == DXFG_V4_MESSAGE_CONNECTOR_STATE_DISCONNECTED; }));

    const auto after = lastErrorOf(thread, connector);
    REQUIRE(after.present);
    CHECK(after.category == DXFG_V4_CONNECTION_ERROR_CONNECT_FAILED);
    CHECK(after.address == "127.0.0.1:1");
    CHECK(after.count >= before.count);
    CHECK(after.time >= before.time);
    CHECK(takeExceptionClassName(thread).empty());
}

TEST_CASE("dxfg_v4_system_shutdown waits for a connector listener call in progress") {
    // Its own isolate: the shutdown cannot be undone.
    Isolate own{};
    REQUIRE(own.thread != nullptr);

    Endpoint feed(own.thread, DXFG_ENDPOINT_ROLE_FEED);
    REQUIRE(feed.ready);

    static StateCalls blocked{true}; // Static: the listener may outlive the test case if it fails.
    ConnectorListener listener(own.thread, blocked);

    REQUIRE(dxfg_v4_DXEndpoint_addMessageConnectorListener(own.thread, feed.endpoint, listener.listener) ==
            DXFG_EXECUTE_SUCCESSFULLY);
    REQUIRE(feed.connect("127.0.0.1:1"));
    REQUIRE(waitFor([] { return blocked.count > 0; })); // The call blocks on the thread of the SDK.

    Attached shuttingDown(own.isolate, &dxfg_v4_system_shutdown);

    std::this_thread::sleep_for(std::chrono::milliseconds(500));
    CHECK_FALSE(shuttingDown.returned());

    blocked.release();
    CHECK(shuttingDown.join() == DXFG_EXECUTE_SUCCESSFULLY);

    const int count = blocked.count;

    std::this_thread::sleep_for(std::chrono::seconds(1));
    CHECK(blocked.count == count);
}

} // namespace test
} // namespace dxfg
