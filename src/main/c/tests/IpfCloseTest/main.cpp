// Copyright (c) 2026 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

// The close of a live instrument profile connection (MDAPI-479): a connection to a live IPF server on a loopback port
// is closed; the connection to the server ends, the collector gets nothing from it afterwards, and the isolate shuts
// down and tears down within the timeout.

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

#include <atomic>
#include <chrono>
#include <cstdint>
#include <cstdio>
#include <cstdlib>
#include <deque>
#include <future>
#include <mutex>
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

void closeSocket(NativeSocket s) {
    closesocket(s);
}
#else
using NativeSocket = int;
constexpr NativeSocket INVALID_NATIVE_SOCKET = -1;
using SocketAddressLength = socklen_t;

void closeSocket(NativeSocket s) {
    close(s);
}
#endif

#ifdef MSG_NOSIGNAL
constexpr int SEND_FLAGS = MSG_NOSIGNAL;
#else
constexpr int SEND_FLAGS = 0;
#endif

constexpr auto TIMEOUT = std::chrono::seconds(20);
constexpr int64_t TIMEOUT_MILLIS = 20000;

constexpr const char *HEADER = "#STOCK::=TYPE,SYMBOL,DESCRIPTION\r\n";

// A live IPF server on a loopback port for one client: it answers the request with a chunked live stream (one profile
// and the complete marker), then sends a heartbeat chunk every 500 ms and the profiles queued by send(). It notes when
// the client closes the connection.
class LiveIpfServer {
    NativeSocket listener_ = INVALID_NATIVE_SOCKET;
    std::uint16_t port_ = 0;
    std::mutex mutex_{};
    std::deque<std::string> queue_{};
    std::atomic<bool> stop_{false};
    std::atomic<bool> clientClosed_{false};
    std::thread thread_{};

  public:
    LiveIpfServer() {
#ifdef _WIN32
        WSADATA wsaData{};

        if (WSAStartup(MAKEWORD(2, 2), &wsaData) != 0) {
            throw std::runtime_error("LiveIpfServer: WSAStartup failed");
        }
#endif

        listener_ = socket(AF_INET, SOCK_STREAM, IPPROTO_TCP);

        sockaddr_in address{};
        address.sin_family = AF_INET;
        address.sin_addr.s_addr = htonl(INADDR_LOOPBACK);
        address.sin_port = 0;

        SocketAddressLength length = sizeof(address);

        if (listener_ == INVALID_NATIVE_SOCKET ||
            bind(listener_, reinterpret_cast<sockaddr *>(&address), sizeof(address)) != 0 ||
            getsockname(listener_, reinterpret_cast<sockaddr *>(&address), &length) != 0 || listen(listener_, 1) != 0) {
            throw std::runtime_error("LiveIpfServer: socket(), bind() or listen() failed");
        }

        port_ = ntohs(address.sin_port);
        thread_ = std::thread([this] { serve(); });
    }

    LiveIpfServer(const LiveIpfServer &) = delete;
    LiveIpfServer &operator=(const LiveIpfServer &) = delete;

    ~LiveIpfServer() {
        stop_ = true;
#ifndef _WIN32
        // Closing a listening socket does not wake a thread blocked in accept() on Linux and macOS.
        shutdown(listener_, SHUT_RDWR);
#endif
        closeSocket(listener_);

        if (thread_.joinable()) {
            thread_.join();
        }
    }

    std::string address() const {
        return "127.0.0.1:" + std::to_string(port_);
    }

    void send(const std::string &symbol) {
        std::lock_guard<std::mutex> lock(mutex_);

        queue_.push_back(symbol);
    }

    bool clientClosed() const {
        return clientClosed_;
    }

    bool waitClientClosed(std::chrono::milliseconds timeout) const {
        for (auto end = std::chrono::steady_clock::now() + timeout; std::chrono::steady_clock::now() < end;) {
            if (clientClosed_) {
                return true;
            }

            std::this_thread::sleep_for(std::chrono::milliseconds(50));
        }

        return clientClosed_;
    }

  private:
    void serve() {
        const NativeSocket client = accept(listener_, nullptr, nullptr);

        if (client == INVALID_NATIVE_SOCKET) {
            return;
        }

#ifdef SO_NOSIGPIPE
        // macOS has no MSG_NOSIGNAL: a send after the client closed must fail, not raise SIGPIPE.
        int noSigPipe = 1;

        setsockopt(client, SOL_SOCKET, SO_NOSIGPIPE, &noSigPipe, sizeof(noSigPipe));
#endif

        if (readRequest(client) &&
            sendAll(client, "HTTP/1.1 200 OK\r\nContent-Type: text/plain; charset=UTF-8\r\nTransfer-Encoding: "
                            "chunked\r\nX-Live: provided\r\nConnection: close\r\n\r\n") &&
            sendChunk(client, std::string(HEADER) + "STOCK,AAA,A\r\n##COMPLETE\r\n\r\n")) {
            while (!stop_ && !isClosedByClient(client)) {
                if (!sendChunk(client, nextChunk())) {
                    break;
                }
            }
        }

        clientClosed_ = true;
        closeSocket(client);
    }

    std::string nextChunk() {
        std::lock_guard<std::mutex> lock(mutex_);

        if (queue_.empty()) {
            return "\r\n"; // a heartbeat
        }

        const std::string symbol = queue_.front();

        queue_.pop_front();

        return std::string(HEADER) + "STOCK," + symbol + "," + symbol + "\r\n##\r\n\r\n";
    }

    // Waits up to 500 ms for the client: it sends nothing after the request, so a readable socket means it closed.
    static bool isClosedByClient(NativeSocket client) {
        fd_set readable;

        FD_ZERO(&readable);
        FD_SET(client, &readable);

        timeval timeout{0, 500000};

        if (select(static_cast<int>(client + 1), &readable, nullptr, nullptr, &timeout) <= 0) {
            return false;
        }

        char byte = 0;

        return recv(client, &byte, 1, 0) <= 0;
    }

    static bool readRequest(NativeSocket client) {
        std::string request;
        char byte = 0;

        while (request.size() < 4 || request.compare(request.size() - 4, 4, "\r\n\r\n") != 0) {
            if (recv(client, &byte, 1, 0) != 1) {
                return false;
            }

            request.push_back(byte);
        }

        return true;
    }

    static bool sendAll(NativeSocket client, const std::string &data) {
        for (std::size_t sent = 0; sent < data.size();) {
            const auto n = ::send(client, data.data() + sent, static_cast<int>(data.size() - sent), SEND_FLAGS);

            if (n <= 0) {
                return false;
            }

            sent += static_cast<std::size_t>(n);
        }

        return true;
    }

    static bool sendChunk(NativeSocket client, const std::string &data) {
        char size[16];

        std::snprintf(size, sizeof(size), "%zx\r\n", data.size());

        return sendAll(client, size) && sendAll(client, data) && sendAll(client, "\r\n");
    }
};

// The calls of an update listener before and after close(), and whether a profile of BBB arrived.
struct Calls {
    std::atomic<bool> closed{false};
    std::atomic<int> beforeClose{0};
    std::atomic<int> afterClose{0};
    std::atomic<bool> bbbReceived{false};

    bool waitBeforeClose() const {
        for (auto end = std::chrono::steady_clock::now() + TIMEOUT; std::chrono::steady_clock::now() < end;) {
            if (beforeClose > 0) {
                return true;
            }

            std::this_thread::sleep_for(std::chrono::milliseconds(10));
        }

        return beforeClose > 0;
    }
};

// Reads the whole iterator: the collector calls a listener again while its iterator has entries left, so an unread
// entry would come once more, possibly after close().
void onUpdate(graal_isolatethread_t *thread, dxfg_v4_iterable_ip_t *profiles, void *userData) {
    auto *calls = static_cast<Calls *>(userData);

    if (calls->closed) {
        ++calls->afterClose;
    } else {
        ++calls->beforeClose;
    }

    for (;;) {
        int32_t hasNext = 0;
        dxfg_v4_instrument_profile_data_t *profile = nullptr;

        if (dxfg_v4_Iterable_InstrumentProfile_hasNext(thread, profiles, &hasNext) != DXFG_EXECUTE_SUCCESSFULLY ||
            hasNext == 0 ||
            dxfg_v4_Iterable_InstrumentProfile_next(thread, profiles, &profile) != DXFG_EXECUTE_SUCCESSFULLY ||
            profile == nullptr) {
            return;
        }

        if (profile->symbol != nullptr && std::string(profile->symbol) == "BBB") {
            calls->bbbReceived = true;
        }

        dxfg_v4_instrument_profile_data_release(thread, profile);
    }
}

} // namespace

// With QDS 3.356 the handler thread stayed blocked in the socket read after close(), and graal_tear_down_isolate()
// waited for it as long as the server kept the stream open (QD-1802, fixed in QDS 3.357).
TEST_CASE("close of a live IPF connection ends its connection, and the isolate tears down") {
    LiveIpfServer server;
    graal_isolate_t *isolate = nullptr;
    graal_isolatethread_t *thread = nullptr;

    REQUIRE(graal_create_isolate(nullptr, &isolate, &thread) == 0);

    Calls calls{};
    dxfg_v4_ipf_collector_t *collector = nullptr;
    dxfg_v4_ipf_update_listener_t *listener = nullptr;
    dxfg_v4_ipf_connection_t *connection = nullptr;
    int32_t completed = 0;
    auto state = static_cast<dxfg_v4_ipf_connection_state_t>(-1);

    REQUIRE(dxfg_v4_InstrumentProfileCollector_new(thread, &collector) == DXFG_EXECUTE_SUCCESSFULLY);
    REQUIRE(dxfg_v4_InstrumentProfileUpdateListener_new(thread, &onUpdate, &calls, &listener) ==
            DXFG_EXECUTE_SUCCESSFULLY);
    REQUIRE(dxfg_v4_InstrumentProfileCollector_addUpdateListener(thread, collector, listener) ==
            DXFG_EXECUTE_SUCCESSFULLY);
    REQUIRE(dxfg_v4_InstrumentProfileConnection_createConnection(thread, server.address().c_str(), collector,
                                                                 &connection) == DXFG_EXECUTE_SUCCESSFULLY);
    REQUIRE(dxfg_v4_InstrumentProfileConnection_start(thread, connection) == DXFG_EXECUTE_SUCCESSFULLY);
    REQUIRE(dxfg_v4_InstrumentProfileConnection_waitUntilCompleted(thread, connection, TIMEOUT_MILLIS, &completed) ==
            DXFG_EXECUTE_SUCCESSFULLY);
    REQUIRE(completed == 1);
    // The collector notifies its listeners on its executor: wait for the snapshot before close().
    REQUIRE(calls.waitBeforeClose());

    calls.closed = true;
    REQUIRE(dxfg_v4_InstrumentProfileConnection_close(thread, connection) == DXFG_EXECUTE_SUCCESSFULLY);
    REQUIRE(dxfg_v4_InstrumentProfileConnection_getState(thread, connection, &state) == DXFG_EXECUTE_SUCCESSFULLY);
    CHECK(state == DXFG_IPF_CONNECTION_STATE_CLOSED);

    // Data that reaches the closed connection is not passed to the collector.
    server.send("BBB");
    CHECK(server.waitClientClosed(TIMEOUT));

    // A barrier: the collector notifies its listeners in order on one thread, and a new listener of a non-empty
    // collector is called once, after every notification queued before it.
    Calls barrier{};
    dxfg_v4_ipf_update_listener_t *barrierListener = nullptr;

    REQUIRE(dxfg_v4_InstrumentProfileUpdateListener_new(thread, &onUpdate, &barrier, &barrierListener) ==
            DXFG_EXECUTE_SUCCESSFULLY);
    REQUIRE(dxfg_v4_InstrumentProfileCollector_addUpdateListener(thread, collector, barrierListener) ==
            DXFG_EXECUTE_SUCCESSFULLY);
    REQUIRE(barrier.waitBeforeClose());
    CHECK(calls.afterClose == 0);
    CHECK_FALSE(calls.bbbReceived);
    CHECK_FALSE(barrier.bbbReceived);

    REQUIRE(dxfg_v4_JavaObjectHandler_release(thread, &barrierListener->handler) == DXFG_EXECUTE_SUCCESSFULLY);
    REQUIRE(dxfg_v4_JavaObjectHandler_release(thread, &listener->handler) == DXFG_EXECUTE_SUCCESSFULLY);
    REQUIRE(dxfg_v4_JavaObjectHandler_release(thread, &connection->handler) == DXFG_EXECUTE_SUCCESSFULLY);
    REQUIRE(dxfg_v4_JavaObjectHandler_release(thread, &collector->handler) == DXFG_EXECUTE_SUCCESSFULLY);
    REQUIRE(dxfg_v4_system_shutdown(thread) == DXFG_EXECUTE_SUCCESSFULLY);

    // graal_tear_down_isolate() must run on the thread that owns the isolate thread, so a watchdog ends the process
    // if it does not return in time: a thread blocked in it cannot be joined.
    std::promise<void> tornDown;
    auto tearDownDone = tornDown.get_future();
    std::thread watchdog([&tearDownDone] {
        if (tearDownDone.wait_for(TIMEOUT) != std::future_status::ready) {
            std::fprintf(stderr, "graal_tear_down_isolate() did not return in time\n");
            std::fflush(stderr);
            std::_Exit(1);
        }
    });

    const int tearDownResult = graal_tear_down_isolate(thread);

    tornDown.set_value();
    watchdog.join();
    CHECK(tearDownResult == 0);
}

} // namespace test
} // namespace dxfg
